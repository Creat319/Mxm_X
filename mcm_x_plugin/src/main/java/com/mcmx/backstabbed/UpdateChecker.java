package com.mcmx.backstabbed;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Random;

/**
 * 版本更新检查。
 *
 * <ul>
 *     <li>启动后、以及每 {@code check-interval-minutes} 分钟异步请求一次版本接口。</li>
 *     <li>普通更新：只在每天 {@code daily-reminder}（默认 12:00）随机选一条文案提醒。</li>
 *     <li>紧急更新：接口返回 {@code "最新版本_有严重bug的版本"}（例如 {@code "4.2_4.1"}），
 *         且当前版本 ≤ 有 bug 的版本时，每小时检测到就立即警告。</li>
 * </ul>
 */
public class UpdateChecker {

    private static final List<String> REMINDERS = List.of(
            "你的插件该换新衣服啦～更新一下，体验更丝滑。 https://github.com/Creat319/Mxm_X",
            "旧版本：我还能再战。服务器：不，你不能。请更新。 https://github.com/Creat319/Mxm_X",
            "插件更新了，不是我想催你，是新功能它自己等不及了。 https://github.com/Creat319/Mxm_X",
            "别让旧插件拖你后腿，点一下更新，快乐加倍。 https://github.com/Creat319/Mxm_X"
    );

    private final McmxPlugin plugin;
    private final Random random = new Random();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    public boolean enabled = true;
    public String apiUrl = "http://carovo.shop/api/version.php?format=json";
    public int checkIntervalMinutes = 60;
    public String dailyReminder = "12:00";

    private volatile String latestVersion;
    private volatile boolean urgent;
    private volatile boolean updateAvailable;
    private volatile LocalDate lastReminderDate;

    public UpdateChecker(McmxPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        enabled = plugin.getConfig().getBoolean("update-checker.enabled", true);
        apiUrl = plugin.getConfig().getString("update-checker.api-url",
                "http://carovo.shop/api/version.php?format=json");
        checkIntervalMinutes = Math.max(1, plugin.getConfig().getInt("update-checker.check-interval-minutes", 60));
        dailyReminder = plugin.getConfig().getString("update-checker.daily-reminder", "12:00");
    }

    public void start() {
        if (!enabled) {
            return;
        }
        // 每 check-interval-minutes 分钟异步检查一次；启动后 5 秒先查一次
        long periodTicks = 20L * 60L * checkIntervalMinutes;
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkNow, 100L, periodTicks);
        // 每分钟判断一次是否到了每日提醒时间
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickDaily, 20L * 60L, 20L * 60L);
    }

    /** 手动触发一次异步检查（/mcmx version check）。 */
    public void requestCheck() {
        if (enabled) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, this::checkNow);
        }
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public boolean isUrgent() {
        return urgent;
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    /** 异步请求版本接口。 */
    private void checkNow() {
        if (!enabled) {
            return;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "McmX/" + currentVersion())
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return;
            }
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            int code = json.has("code") ? json.get("code").getAsInt() : -1;
            if (code != 0) {
                return;
            }
            String remote = json.has("version") ? json.get("version").getAsString() : null;
            if (remote == null || remote.isBlank()) {
                return;
            }
            evaluate(remote.trim());
        } catch (Exception ex) {
            // 网络异常忽略，不打扰游戏
            plugin.getLogger().fine("更新检查失败: " + ex.getMessage());
        }
    }

    /** 解析远端版本号并记录状态；紧急则立即广播。 */
    private void evaluate(String remote) {
        String latest = remote;
        boolean urgentNow = false;
        if (remote.contains("_")) {
            String[] parts = remote.split("_", 2);
            latest = parts[0].trim();
            String buggy = parts[1].trim();
            // 当前版本 <= 有严重 bug 的版本 -> 紧急
            if (compareVersions(currentVersion(), buggy) <= 0) {
                urgentNow = true;
            }
        }

        boolean newer = compareVersions(currentVersion(), latest) < 0;
        this.latestVersion = latest;
        this.urgent = urgentNow;
        this.updateAvailable = newer || urgentNow;

        if (urgentNow) {
            // 检测到紧急 bug 立即警告（切回主线程广播）
            Bukkit.getScheduler().runTask(plugin, () -> broadcast(true));
        }
    }

    /** 每分钟判断一次是否到了每日提醒时间（12:00）。 */
    private void tickDaily() {
        if (!enabled || !updateAvailable) {
            return;
        }
        LocalTime now = LocalTime.now();
        LocalTime target = parseTime(dailyReminder);
        if (target == null || now.getHour() != target.getHour() || now.getMinute() != target.getMinute()) {
            return;
        }
        LocalDate today = LocalDate.now();
        if (today.equals(lastReminderDate)) {
            return;
        }
        lastReminderDate = today;
        broadcast(urgent);
    }

    private void broadcast(boolean urgentMode) {
        if (!enabled) {
            return;
        }
        String message = REMINDERS.get(random.nextInt(REMINDERS.size()));
        String latest = latestVersion != null ? latestVersion : currentVersion();
        String prefix;
        if (urgentMode) {
            prefix = "[McmX 紧急] 当前版本 " + currentVersion()
                    + " 存在严重 bug，请立即更新到 " + latest + "+！";
        } else {
            prefix = "[McmX] 发现新版本 " + latest + "（当前 " + currentVersion() + "）：";
        }
        Component component = Component.text(prefix + " " + message,
                urgentMode ? NamedTextColor.RED : NamedTextColor.GOLD);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(component);
        }
        Bukkit.getConsoleSender().sendMessage(component);
    }

    private String currentVersion() {
        return plugin.getDescription().getVersion();
    }

    private LocalTime parseTime(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return LocalTime.parse(raw.trim());
        } catch (Exception ex) {
            return null;
        }
    }

    /** 比较版本号，a > b 返回正数。支持 "4.2"、"4.2.1" 这种格式。 */
    public static int compareVersions(String a, String b) {
        String[] left = (a == null ? "0" : a).split("\\.");
        String[] right = (b == null ? "0" : b).split("\\.");
        int length = Math.max(left.length, right.length);
        for (int i = 0; i < length; i++) {
            int x = i < left.length ? leadingInt(left[i]) : 0;
            int y = i < right.length ? leadingInt(right[i]) : 0;
            if (x != y) {
                return Integer.compare(x, y);
            }
        }
        return 0;
    }

    private static int leadingInt(String raw) {
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (Character.isDigit(c)) {
                digits.append(c);
            } else {
                break;
            }
        }
        if (digits.length() == 0) {
            return 0;
        }
        try {
            return Integer.parseInt(digits.toString());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
