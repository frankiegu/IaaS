package appcloud.dbproxy.mysql;

import java.util.ArrayList;
import java.util.List;
import appcloud.common.model.Cluster;
import appcloud.common.model.Host;
import appcloud.common.model.Instance.InstanceTypeEnum;
import appcloud.common.model.Load;
import appcloud.common.model.NetworkLoad;
import appcloud.common.proxy.HostProxy;
import appcloud.common.util.query.QueryObject;
import appcloud.dbproxy.mysql.dao.ClusterDAO;
import appcloud.dbproxy.mysql.dao.HostDAO;
import appcloud.dbproxy.mysql.dao.InstanceDAO;
import appcloud.dbproxy.mysql.dao.LoadDAO;
import appcloud.dbproxy.mysql.dao.NetworkLoadDAO;
import appcloud.dbproxy.mysql.model.HostTable;
import org.apache.log4j.Logger;

public class MySQLHostProxy implements HostProxy{
	private static HostDAO dao = new HostDAO();
	private final static Logger logger = Logger.getLogger(MySQLHostProxy.class);
	
	/**
	 * 取得所有节点信息
	 * @param withCluster 同时取得集群信息
	 * @param withLoad 同时取得主机负载信息（包括Load和NetworkLoad）
	 * @param withNum 同时取得计算节点上承载的j2ee实例数和vm实例数
	 * @return
	 */
	//done
	@Override
	public List<? extends Host> findAll(boolean withCluster, boolean withLoad, boolean withNum) throws Exception{
		return findAll( withCluster,  withLoad,  withNum, 0, 0);
		
	}

	/**
	 * 分页取得节点信息
	 * @param withCluster 同时取得集群信息
	 * @param withLoad 同时取得主机负载信息（包括Load和NetworkLoad）
	 * @param withNum 同时取得计算节点上承载的j2ee实例数和vm实例数
	 * @param page 第几页，0代表不分页
	 * @param size 每页大小，0代表不分页
	 * @return
	 */
	//done
	@Override
	public List<? extends Host> findAll(boolean withCluster, boolean withLoad, boolean withNum, 
			Integer page, Integer size) throws Exception {
		List<? extends Host> hosts = dao.findAll(page, size);
		for (Host host : hosts) {
			fillUpHost(host, withCluster, withLoad, withNum);
		}
		return hosts;
	}

	/**
	 * 取得总记录条数
	 * @return
	 */
	//done
	@Override
	public long countAll() throws Exception {
		return dao.countAll();
	}

	/**
	 * 搜索全部
	 *  properties 查询条件，用户自己构造
	 * @param withCluster 同时取得集群信息
	 * @param withLoad 同时取得主机负载信息（包括Load和NetworkLoad）
	 * @param withNum 同时取得计算节点上承载的j2ee实例数和vm实例数
	 * @return
	 */
	//done
	@Override
	public List<? extends Host> searchAll(QueryObject<Host> queryObject,
			boolean withCluster, boolean withLoad, boolean withNum) throws Exception {
		return searchAll(queryObject,withCluster,withLoad, withNum,0,0);
	}

	/**
	 * 分页搜索
	 *  properties 查询条件，用户自己构造
	 * @param withCluster 同时取得集群信息
	 * @param withLoad 同时取得主机负载信息（包括Load和NetworkLoad）
	 * @param withNum 同时取得计算节点上承载的j2ee实例数和vm实例数
	 * @param page 第几页，0代表不分页
	 * @param size 每页大小，0代表不分页
	 * @return
	 */
	//done
	@Override
	public List<? extends Host> searchAll(QueryObject<Host> queryObject,
			boolean withCluster, boolean withLoad, boolean withNum, 
			Integer page, Integer size) throws Exception {
		
		List<? extends Host> hosts = dao.findByProperties(queryObject, page, size);
		for (Host host : hosts) {
			fillUpHost(host, withCluster, withLoad, withNum);
		}
		return hosts;
	}

	/**
	 * 取得查询记录条数
	 * @return
	 */
	@Override
	public long countSearch(QueryObject<Host> queryObject) throws Exception {
		return dao.countByProperties(queryObject);
	}
	
	/**
	 * 通过uuid取得某个host信息
	 * @param uuid hostUuid
	 * @param withCluster 同时取得集群信息
	 * @param withLoad 同时取得主机负载信息（包括Load和NetworkLoad）
	 * @param withNum 同时取得计算节点上承载的j2ee实例数和vm实例数
	 * @return
	 */
	//done
	@Override
	public Host getByUuid(String uuid, boolean withCluster, boolean withLoad, boolean withNum) throws Exception{
		List<? extends Host> hosts = dao.findByProperty("uuid", uuid);
		if (hosts.size() != 0) {
			Host host = hosts.get(0);
			fillUpHost(host, withCluster, withLoad, withNum);
			return host;
		} else {
			return null;
		}
	}
	
	/**
	 * 保存一个主机节点
	 * @param host
	 * @return
	 */
	//done
	@Override
	public void save(Host host) throws Exception{
		host.setClusterId(0);
		dao.save(new HostTable(host));
	}
	
	/**
	 * 更新一个主机节点信息
	 * @param host
	 * @return
	 */
	//done
	@Override
	public Host update(Host host) throws Exception{
		return dao.update(new HostTable(host));
	}
	
	/**
	 * 删除一个主机节点
	 * @param uuid 主机的uuid
	 * @return
	 */
	//done
	@Override
	public void deleteByUuid(String uuid) throws Exception{
		dao.deleteByProperty("uuid", uuid);
	}
	/**
	 * 获取一个主机节点的其他属性
	 * @param host 主机节点
	 * @param withCluster 同时取得集群信息
	 * @param withLoad 同时取得主机负载信息（包括Load和NetworkLoad）
	 * @param withNum 同时取得计算节点上承载的j2ee实例数和vm实例数
	 * @return
	 */
	private void fillUpHost(Host host, boolean withCluster, boolean withLoad, boolean withNum) {
		if (withCluster) {
			Cluster cluster = (new ClusterDAO()).findById( host.getClusterId() );
			host.setCluster(cluster);
		}
		if (withLoad) {
			Load load = (new LoadDAO()).getCurLoadByUuid(host.getUuid() );
			host.setLoad(load);
			NetworkLoad networkLoad = (new NetworkLoadDAO()).getCurNetLoadByUuid( host.getUuid() );
			host.setNetworkLoad(networkLoad);
		}
		if (withNum) {
			long j2eeNum = (new InstanceDAO()).countByProperty2("hostUuid", host.getUuid(),
					"type", InstanceTypeEnum.J2EE);
			host.setJ2eeInstanceNum((int)j2eeNum);
			
			long vmNum = (new InstanceDAO()).countByProperty2("hostUuid", host.getUuid(),
					"type", InstanceTypeEnum.VM);
			host.setVmInstanceNum((int)vmNum);
		}
	}

	@Override
	public List<? extends Host> findByType(String type) throws Exception{
		List<? extends Host> hosts = new ArrayList<Host>();
		if (type.equals("COMPUTE_NODE")){
			hosts = dao.findByProperty("type",Host.HostTypeEnum.COMPUTE_NODE);
		} else if (type.equals("FUNCTION_NODE")){
			hosts = dao.findByProperty("type",Host.HostTypeEnum.FUNCTION_NODE);
		} else if (type.equals("STORAGE_NODE")){
			hosts = dao.findByProperty("type",Host.HostTypeEnum.STORAGE_NODE);
		} else {
			logger.error("find by type"+type+"error");
		}
		return hosts;
	}
}
