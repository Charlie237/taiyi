package io.github.charlie237.taiyi.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.*;
import java.util.Enumeration;
import java.util.regex.Pattern;

/**
 * 网络工具类
 */
@Slf4j
public class NetworkUtils {
    
    private static final Pattern IP_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$"
    );
    
    private static final Pattern PORT_PATTERN = Pattern.compile(
            "^([1-9]|[1-9]\\d{1,3}|[1-5]\\d{4}|6[0-4]\\d{3}|65[0-4]\\d{2}|655[0-2]\\d|6553[0-5])$"
    );
    
    /**
     * 验证IP地址格式
     */
    public static boolean isValidIp(String ip) {
        return StringUtils.isNotBlank(ip) && IP_PATTERN.matcher(ip).matches();
    }
    
    /**
     * 验证端口号
     */
    public static boolean isValidPort(Integer port) {
        return port != null && port > 0 && port <= 65535;
    }
    
    /**
     * 验证端口号字符串
     */
    public static boolean isValidPort(String port) {
        return StringUtils.isNotBlank(port) && PORT_PATTERN.matcher(port).matches();
    }
    
    /**
     * 检查端口是否可用
     */
    public static boolean isPortAvailable(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 检查端口是否可用（指定IP）
     */
    public static boolean isPortAvailable(String host, int port) {
        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.bind(new InetSocketAddress(host, port));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 获取本机IP地址
     */
    public static String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }
                
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取本机IP地址失败", e);
        }
        return "127.0.0.1";
    }
    
    /**
     * 获取公网IP地址
     */
    public static String getPublicIpAddress() {
        try {
            URL url = new URL("http://checkip.amazonaws.com/");
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(url.openStream()))) {
                return reader.readLine().trim();
            }
        } catch (Exception e) {
            log.warn("获取公网IP地址失败", e);
            return getLocalIpAddress();
        }
    }
    
    /**
     * 测试网络连通性
     */
    public static boolean isReachable(String host, int port, int timeout) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeout);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 计算网络延迟
     */
    public static long calculateLatency(String host, int port) {
        long startTime = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 5000);
            return System.currentTimeMillis() - startTime;
        } catch (Exception e) {
            return -1;
        }
    }
    
    /**
     * 判断是否为内网IP
     */
    public static boolean isPrivateIp(String ip) {
        if (!isValidIp(ip)) {
            return false;
        }
        
        String[] parts = ip.split("\\.");
        int first = Integer.parseInt(parts[0]);
        int second = Integer.parseInt(parts[1]);
        
        // 10.0.0.0/8
        if (first == 10) {
            return true;
        }
        
        // 172.16.0.0/12
        if (first == 172 && second >= 16 && second <= 31) {
            return true;
        }
        
        // 192.168.0.0/16
        if (first == 192 && second == 168) {
            return true;
        }
        
        // 127.0.0.0/8 (loopback)
        if (first == 127) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 格式化带宽
     */
    public static String formatBandwidth(long bytes) {
        if (bytes < 1024) {
            return bytes + " B/s";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB/s", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB/s", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB/s", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    /**
     * 格式化数据大小
     */
    public static String formatDataSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
}
