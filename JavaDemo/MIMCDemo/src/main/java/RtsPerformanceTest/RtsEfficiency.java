package RtsPerformanceTest;

import com.xiaomi.mimc.MIMCOnlineStatusListener;
import com.xiaomi.mimc.MIMCUser;
import com.xiaomi.mimc.common.MIMCConstant;
import com.xiaomi.mimc.data.DataPriority;
import com.xiaomi.mimc.data.RtsChannelType;
import com.xiaomi.mimc.data.RtsDataType;
import com.xiaomi.mimc.logger.MIMCLog;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.MIMCCaseMessageHandler;
import utils.MIMCCaseTokenFetcher;
import utils.RtsMessageData;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentMap;

public class RtsEfficiency {
    private static final Logger logger = LoggerFactory.getLogger(RtsEfficiency.class);

    public static final long appId = 2882303761517669588l;
    public static final String appKey = "5111766983588";
    public static final String appSecurity = "b0L3IOz/9Ob809v8H2FbVg==";
    public static final String urlForToken = "https://mimc.chat.xiaomi.net/api/account/token";

    private static int TIME_OUT = 5000;
    private final String cachePath = "cacheFileRtsEfficiency";

    private MIMCUser rtsUser1;
    private MIMCUser rtsUser2;

    private final String appAccount1 = "prod_rts_efficiency_account1";
    private final String appAccount2 = "prod_rts_efficiency_account2";

    private MIMCCaseMessageHandler msgHandler1 = new MIMCCaseMessageHandler();
    private MIMCCaseMessageHandler msgHandler2 = new MIMCCaseMessageHandler();

    private RtsEfficiencyHandler callEventHandler1 = new RtsEfficiencyHandler();
    private RtsEfficiencyHandler callEventHandler2 = new RtsEfficiencyHandler();

    public RtsEfficiency() throws Throwable {
    }

    public void setup() throws Throwable {
        File directory = new File(".");
        String currentPath = directory.getCanonicalPath();
        currentPath = currentPath + System.getProperty("file.separator") + cachePath;

        MIMCLog.setLogger(new com.xiaomi.mimc.logger.Logger() {
            public void d(String s, String s1) {
                logger.debug("{}, {}", s, s1);
            }

            public void d(String s, String s1, Throwable throwable) {
                logger.debug("{}, {}", s, s1, throwable);
            }

            public void i(String s, String s1) {
                logger.info("{}, {}", s, s1);
            }

            public void i(String s, String s1, Throwable throwable) {
                logger.info("{}, {}", s, s1, throwable);
            }

            public void w(String s, String s1) {
                logger.warn("{}, {}", s, s1);
            }

            public void w(String s, String s1, Throwable throwable) {
                logger.warn("{}, {}", s, s1, throwable);
            }

            public void e(String s, String s1) {
                logger.error("{}, {}", s, s1);
            }

            public void e(String s, String s1, Throwable throwable) {
                logger.error("{}, {}", s, s1, throwable);
            }
        });
        MIMCLog.setLogPrintLevel(1);

        rtsUser1 = MIMCUser.newInstance(Long.valueOf(appId), appAccount1, currentPath);
        rtsUser1.registerTokenFetcher(new MIMCCaseTokenFetcher(appId, appKey, appSecurity, urlForToken, appAccount1));
        rtsUser1.registerOnlineStatusListener(new MIMCOnlineStatusListener() {
            public void statusChange(MIMCConstant.OnlineStatus status, String errType, String errReason, String errDescription) {
                logger.info("OnlineStatusHandler, Called, isOnline:{}, errType:{}, errReason:{}, errDesc:{}",
                        status, errType, errReason, errDescription);
            }
        });
        rtsUser1.registerMessageHandler(msgHandler1);
        rtsUser1.registerRtsCallHandler(callEventHandler1);

        rtsUser2 = MIMCUser.newInstance(Long.valueOf(appId), appAccount2, currentPath);
        rtsUser2.registerTokenFetcher(new MIMCCaseTokenFetcher(appId, appKey, appSecurity, urlForToken, appAccount2));
        rtsUser2.registerOnlineStatusListener(new MIMCOnlineStatusListener() {
            public void statusChange(MIMCConstant.OnlineStatus status, String errType, String errReason, String errDescription) {
                logger.info("OnlineStatusHandler, Called, isOnline:{}, errType:{}, errReason:{}, errDesc:{}",
                        status, errType, errReason, errDescription);
            }
        });
        rtsUser2.registerMessageHandler(msgHandler2);
        rtsUser2.registerRtsCallHandler(callEventHandler2);
    }

    public void destroy() throws Throwable {
        rtsUser1.logout();
        rtsUser2.logout();

        callEventHandler1.clear();
        callEventHandler2.clear();

        rtsUser1.destroy();
        rtsUser2.destroy();
    }

    @Test
    public void testEfficiency() throws Throwable {
        final int runCount = 5;
        final int dataSize = 100 * 1024;
        Map<Integer, RtsPerformanceData> timeRecords = new TreeMap<Integer, RtsPerformanceData>();

        for (int i = 0; i < runCount; i++) {
            setup();
            testEfficiency(rtsUser1, callEventHandler1, rtsUser2, callEventHandler2, i, dataSize, timeRecords);
            destroy();
        }

        long sumLoginTime1 = 0;
        long sumLoginTime2 = 0;
        long sumDiaCallTime = 0;
        long sumRecvDataTime = 0;
        for (int i : timeRecords.keySet()) {
            logger.info("TEST_TIME_{}, " +
                "\n    DATA SIZE: {}KB" +
                "\n    loginTime1:{} ms" +
                "\n    loginTime2:{} ms" +
                "\n    dialCallTime:{} ms, " +
                "\n    recvDataTime:{}",
                i, dataSize / 1024,
                timeRecords.get(i).getLoginTime1(), timeRecords.get(i).getLoginTime2(),
                timeRecords.get(i).getDiaCallTime(), timeRecords.get(i).getRecvDataTime());
            sumLoginTime1 += timeRecords.get(i).getLoginTime1();
            sumLoginTime2 += timeRecords.get(i).getLoginTime2();
            sumDiaCallTime += timeRecords.get(i).getDiaCallTime();
            sumRecvDataTime += timeRecords.get(i).getRecvDataTime();
        }


        logger.info("\n\nTEST {} TIMES, " +
                "\n    DATA SIZE: {}KB," +
                "\n    avgLoginTime1: {}ms" +
                "\n    avgLoginTime2: {}ms" +
                "\n    avgDiaCallTime: {}ms" +
                "\n    avgRecvDataTime: {}ms",
                runCount, dataSize / 1024,
                sumLoginTime1 / runCount,
                sumLoginTime2 / runCount,
                sumDiaCallTime / runCount,
                sumRecvDataTime / runCount);
    }

    public static void testEfficiency(MIMCUser user1, RtsEfficiencyHandler callEventHandler1,
                                      MIMCUser user2, RtsEfficiencyHandler callEventHandler2,
                                      int idx, int dataSize,
                                      Map<Integer, RtsPerformanceData> timeRecords) throws Throwable {
        long t0, t1, t2, t3, t4, t5, t6;
        Random random = new Random();

        byte[] sendData = new byte[dataSize];
        random.nextBytes(sendData);
        Object context = System.currentTimeMillis();

        callEventHandler1.clear();
        callEventHandler2.clear();

        t0 = System.currentTimeMillis();
        t1 = System.currentTimeMillis();
        t2 = System.currentTimeMillis();

        Set<String> offlineUuids = new HashSet<String>();
        offlineUuids.add(user1.getAppAccount());
        offlineUuids.add(user2.getAppAccount());

        logger.info("\nTest{} timeStamp1 user start login: {}", idx + 1, t0);
        user1.login();
        user2.login();
        for (int j = 0; j < TIME_OUT; j++) {
            if (offlineUuids.size() == 0) break;

            if (offlineUuids.contains(user1.getAppAccount()) && user1.isOnline()) {
                offlineUuids.remove(user1.getAppAccount());
                t1 = System.currentTimeMillis();
            }

            if (offlineUuids.contains(user2.getAppAccount()) && user2.isOnline()) {
                offlineUuids.remove(user2.getAppAccount());
                t2 = System.currentTimeMillis();
            }

            Thread.sleep(1);
        }
        logger.info("\nTest{} timeStamp1 user finish login, cost:{} ms", idx, System.currentTimeMillis() -  t0);
        Assert.assertTrue("LOGIN FAILED", user1.isOnline());
        Assert.assertTrue("LOGIN FAILED", user2.isOnline());

        t3 = System.currentTimeMillis();
        long chatId = user1.dialCall(user2.getAppAccount());
        for (int j = 0; j < TIME_OUT; j++) {
            if (callEventHandler1.getMsgSize(2) > 0) break;
            Thread.sleep(1);
        }
        t4 = System.currentTimeMillis();
        Assert.assertEquals("DIACALL FAILED", RtsEfficiencyHandler.LAUNCH_OK, callEventHandler1.pollCreateResponse(3000).getExtramsg());

        t5 = System.currentTimeMillis();
        int groupId = user1.sendRtsData(chatId, sendData, RtsDataType.AUDIO, DataPriority.P0, false, 3, RtsChannelType.RELAY, context);
        Assert.assertNotEquals("SEND DATA FAILED", -1, groupId);
        for (int j = 0; j < TIME_OUT; j++) {
            if (callEventHandler2.getMsgSize(4) > 0) break;
            Thread.sleep(1);
        }
        t6 = System.currentTimeMillis();
        Assert.assertNotEquals("DATA LOST", 0, callEventHandler2.getMsgSize(4));

        ConcurrentMap<Integer, RtsPerformanceData> recvData = callEventHandler2.pollDataInfo();
        Assert.assertTrue("RECEIVE DATA_ID NOT MATCH", recvData.containsKey(RtsPerformance.byteArrayToInt(sendData)));
        Assert.assertTrue("RECEIVE DATA_CONTENT NOT MATCH",
                Arrays.equals(sendData, recvData.get(RtsPerformance.byteArrayToInt(sendData)).getData()));

        user1.closeCall(chatId);

        RtsMessageData byeRequest = callEventHandler2.pollBye(1000);
        Assert.assertNotNull("BYE_REQUEST IS NULL", byeRequest);
        Assert.assertEquals("CHAT_ID NOT MATCH IN BYE_REQUEST", Long.valueOf(chatId), Long.valueOf(byeRequest.getChatId()));
        Assert.assertEquals("BYE_REASON NOT MATCH IN BYE_REQUEST", true, StringUtils.isEmpty(byeRequest.getExtramsg()));

        RtsMessageData byeResponse = callEventHandler1.pollBye(1000);
        Assert.assertNotNull("BYE_RESPONSE IS NULL", byeResponse);
        Assert.assertEquals("CHAT_ID NOT MATCH IN BYE_RESPONSE", Long.valueOf(chatId), Long.valueOf(byeResponse.getChatId()));
        Assert.assertEquals("BYE_REASON NOT MATCH IN BYE_RESPONSE", "CLOSED_INITIATIVELY", byeResponse.getExtramsg());

        timeRecords.put(idx, new RtsPerformanceData(t1 - t0, t2 - t0, t4 - t3, t6 - t5));
    }
}
