package appcloud.distributed.configmanager;

import appcloud.distributed.util.StringUtil;
import appcloud.distributed.util.XmlUtil;
import com.distributed.common.entity.VmZone;
import com.distributed.common.factory.DbFactory;
import com.distributed.common.service.db.VmZoneService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by lizhenhao on 2017/11/22.
 */

/**
 * 后期的routeInfoTable中应该存储的是zookeeper的地址，发送请求执行过程中，在ClientWapper中进行zookeeper节点的筛选！
 */

public class RouteInfoManager {
    protected final static Logger LOGGER = LoggerFactory.getLogger(RouteInfoManager.class);

    private static volatile RouteInfoManager routeInfoManager;

    public static RouteInfoManager getRouteInfoManager() {
        if (routeInfoManager == null) {
            synchronized (RouteInfoManager.class) {
                routeInfoManager = new RouteInfoManager();
            }
        }
        return routeInfoManager;
    }

    public void IntRouteInfoManager(RouteInfo localInfo, RouteInfo masterInfo) {
        this.masterRouteInfo = masterInfo;
        this.ownRouteInfo = localInfo;
    }

    /**
     * 通过读写锁控制，该map存在并发问题，只能通过该类中的方法进行操作，不能够设置get方法，获取到该map的引用（当然其实可以直接用concurrentHashMap,但使用读写锁锻炼并发的思考能力）
     */
    private Map<String /* datacenter */, RouteInfo> routeInfoTable = new HashMap<String, RouteInfo>();

    private AtomicBoolean mapHasChange = new AtomicBoolean(false);

    private RouteInfo monitorRoute;

    private RouteInfo ownRouteInfo;

    private RouteInfo masterRouteInfo;

    private ReadWriteLock rwlock = new ReentrantReadWriteLock();

    private VmZoneService vmZoneService = DbFactory.getVmZoneService();

    public int size() {
        try {
            rwlock.readLock().lock();
            int size = routeInfoTable.size();
            return size;
        } finally {
            rwlock.readLock().unlock();
        }
    }

    public void clear() {
        routeInfoTable.clear();
    }

    public void putRouteInfo(RouteInfo routeInfo) {
        try {
            rwlock.writeLock().lock();
            routeInfoTable.put(routeInfo.getDataCenter(), routeInfo);
            mapHasChange.compareAndSet(false, true);
        } finally {
            rwlock.writeLock().unlock();
        }
    }

    public void removeRouteInfo(String key) {
        try {
            rwlock.writeLock().lock();
            routeInfoTable.remove(key);
            mapHasChange.compareAndSet(false, true);
        } catch (Exception e) {
            LOGGER.error("remove routeinfo fail ,addr is {}", key);
        } finally {
            rwlock.writeLock().unlock();
        }
    }

    public RouteInfo getRouteInfo(final String key) {
        rwlock.readLock().lock();
        RouteInfo routeInfo = routeInfoTable.get(key);
        rwlock.readLock().unlock();
        return routeInfo;
    }

    public List<RouteInfo> getAllRouteInfo() {
        List<RouteInfo> result = new ArrayList<RouteInfo>();
        try {
            rwlock.readLock().lock();
            for (Map.Entry<String, RouteInfo> entry : routeInfoTable.entrySet()) {
                RouteInfo routeInfoConfig = entry.getValue();
                result.add(routeInfoConfig);
            }
            return result;
        } finally {
            rwlock.readLock().unlock();
        }
    }

    public boolean containRouteInfo(String key) {
        try {
            rwlock.readLock().lock();
            return routeInfoTable.containsKey(key);
        } finally {
            rwlock.readLock().unlock();
        }
    }

    /**
     * 全量更新：这里说明一下，之所以需要这么做是因为，如果注册表内部的数据被改变了（后期的该模块高可用可能导致，IP之类的数据改变。），那么从size上是看不出来的，因此需要对之前的注册表进行对比，如果发现uuid和addr都改变了，那么就要设置mapHasChange为true。保证监控对象是最新的！
     */
    public void updateRouteInfo(List<RouteInfo> list) {

//        //测试用，直接将标志位置为true
//        mapHasChange.compareAndSet(false,true);

        if (list.size() != routeInfoTable.size()) mapHasChange.compareAndSet(false, true);
        try {
            rwlock.writeLock().lock();
            Map<String, RouteInfo> tmpRouteInfoTable = new HashMap<String, RouteInfo>(routeInfoTable);
            routeInfoTable.clear();
            for (RouteInfo routeInfo : list) {
                String dataCenter = routeInfo.getDataCenter();
                String addr = routeInfo.getAddr();
                String uuid = routeInfo.getUuid();
                RouteInfo prev = tmpRouteInfoTable.get(dataCenter);
                if (prev != null) {
                    if (!prev.getAddr().equals(addr) || !prev.getUuid().equals(uuid))
                        mapHasChange.compareAndSet(false, true);
                } else mapHasChange.compareAndSet(false, true);
                routeInfoTable.put(dataCenter, routeInfo);
            }
        } catch (Exception e) {
            LOGGER.error("update all routeInfo fail");
        } finally {
            rwlock.writeLock().unlock();
        }
    }

    /**
     * 比较IP数值大小来决定监控谁（选择差值最小的，大于自己的IP作为心跳检测对象）
     * 其实没必要每次计算，从map中有remove操作，再重新计算。
     *
     * @return
     */
    public RouteInfo produceMonitorRouteInfo() {
        if (mapHasChange.getAndSet(false) || monitorRoute == null) {
            try {
                LOGGER.info("change monitor node");
                int minHashCode = Integer.MAX_VALUE;
                long min = Long.MAX_VALUE;
                long ownHash = ownRouteInfo.getUuid().hashCode();
                RouteInfo minIpRouteInfo = null;
                RouteInfo currentRouteInfo = null;
                rwlock.readLock().lock();
                for (Map.Entry<String, RouteInfo> entry : routeInfoTable.entrySet()) {
                    RouteInfo routeInfo = entry.getValue();
                    int hashcode = routeInfo.getUuid().hashCode();
                    if (hashcode > ownHash) {
                        if (hashcode - ownHash < min) {
                            min = hashcode - ownHash;
                            currentRouteInfo = routeInfo;
                        }
                    }
                    if (hashcode < minHashCode && hashcode != ownHash) {
                        minIpRouteInfo = routeInfo;
                        minHashCode = hashcode;
                    }
                }
                if (min == Long.MAX_VALUE) {
                    monitorRoute = minIpRouteInfo;
                    LOGGER.info("监控对象为值最小" + minIpRouteInfo.getDataCenter());
                } else {
                    monitorRoute = currentRouteInfo;
                }
                if (monitorRoute != null) {
                    LOGGER.info("monitor node ip change to:" + monitorRoute.getIpAddress());
                }
                return monitorRoute;

            } catch (Exception e) {
                LOGGER.error("acquire monitor routeInfo fail {}", e);
                return null;
            } finally {
                rwlock.readLock().unlock();
            }
        } else {
            return monitorRoute;
        }

    }

    /**
     * 将数据中心的信息注册到数据库中
     */
    public void initZone() {
        String zoneId = ownRouteInfo.getDataCenter();
        if (vmZoneService.findByZoneId(zoneId) != null) {
            LOGGER.info("该数据中心已存在， zoneId: " + zoneId);
            return;
        }
        VmZone vmZone = new VmZone(null, zoneId, "BEIJING", zoneId);
        vmZoneService.add(vmZone);
        LOGGER.info("插入成功， zoneId: " + zoneId);
    }

    /**
     * master相关
     *
     * @return
     */
    public String getMasterIp() {
        return masterRouteInfo.getIpAddress();
    }

    public String getMasterPort() {
        return masterRouteInfo.getPort();
    }

    public String getMasterAddr() {
        return masterRouteInfo.getAddr();
    }

    public String getMasterDomain() {
        return masterRouteInfo.getDomainName();
    }

    public void updateMaster(String ip, String port, String domainName, String dataCenter, String uuid) {
        masterRouteInfo = new RouteInfo(ip, port, domainName, StringUtil.address(ip, port), dataCenter, uuid);
    }

    public void updateMaster(RouteInfo master) {
        masterRouteInfo = master;
    }

    /**
     * local相关
     *
     * @return
     */
    public String getOwnIp() {
        return ownRouteInfo.getIpAddress();
    }

    public String getOwnPort() {
        return ownRouteInfo.getPort();
    }

    public String getOwnDomainName() {
        return ownRouteInfo.getDomainName();
    }

    public String getOwnAddr() {
        return ownRouteInfo.getAddr();
    }

    public String getOwnDataCenter() {
        return ownRouteInfo.getDataCenter();
    }

    public RouteInfo getMonitorRoute() {
        return monitorRoute;
    }

    public RouteInfo getMasterRouteInfo() {
        return masterRouteInfo;
    }

    public RouteInfo getOwnRouteInfo() {
        return ownRouteInfo;
    }
}
