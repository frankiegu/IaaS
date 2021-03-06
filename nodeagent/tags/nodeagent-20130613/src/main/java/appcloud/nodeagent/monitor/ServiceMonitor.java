package appcloud.nodeagent.monitor;

import java.sql.Timestamp;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import appcloud.common.model.Host;
import appcloud.common.model.Service;
import appcloud.common.model.Service.ServiceStatus;
import appcloud.common.model.Service.ServiceTypeEnum;
import appcloud.common.proxy.ServiceProxy;
import appcloud.common.util.ConnectionFactory;
import appcloud.nodeagent.util.AppUtils;
import appcloud.nodeagent.util.ConfigReader;
import appcloud.nodeagent.util.ServiceManager;
import appcloud.nodeagent.util.ServiceUnit;
import appcloud.rpc.tip.TipRPCClient;

public class ServiceMonitor implements Runnable {

	private static final Logger logger = Logger.getLogger(ServiceMonitor.class);
	private Map<String,ServiceUnit> services;
	private ServiceManager sm;
	private ServiceProxy serviceProxy;
	private Host host;
	private boolean isStarted;
	private Service monitorService;
	
	public ServiceMonitor() {
		isStarted = false;
		services = ConfigReader.readFromJson(ServiceManager.class.getClassLoader().getResource("naservices.json"));
	}
	
	@Override
	public void run() {
		
		if (!isStarted) {
			saveMonitorService();
			isStarted = true;
		}

		monitorService.setZoneId(host.getAvailabilityZoneId());
		monitorService.setClusterId(host.getClusterId());
		monitorService.setServiceStatus(ServiceStatus.RUNNING);
		monitorService.setUpdateTime(new Timestamp(System.currentTimeMillis()));

		logger.debug("udpate " + monitorService);
		serviceProxy.update(monitorService);

		sm = ServiceManager.getInstance();
		for (Entry<String, ServiceUnit> entry : services.entrySet()) {
			updateService(entry.getKey(), entry.getValue());
		}
	}

	private void saveMonitorService() {
		Host host = (Host) AppUtils.getApplicationContext().getBean("host");
		if (host.getId() == null) {
			AppUtils.saveOrUpdateHost();
		}
		
		monitorService = serviceProxy.getUniqueService(host.getIp(), 0, false);
		if (monitorService == null) {
			monitorService = new Service();
			monitorService.setHostId(host.getId());
			monitorService.setHostIp(host.getIp());
			monitorService.setMonitorPort(0);
			monitorService.setHostUuid(host.getUuid());
			monitorService.setZoneId(host.getAvailabilityZoneId());
			monitorService.setClusterId(host.getClusterId());
			monitorService.setType(ServiceTypeEnum.NODE_MONITOR);
			monitorService.setServiceStatus(ServiceStatus.RUNNING);
			monitorService.setUpdateTime(new Timestamp(System.currentTimeMillis()));
			serviceProxy.save(monitorService);
			monitorService = serviceProxy.getUniqueService(host.getIp(), 0, false);
			logger.debug("after save monitorService " + monitorService);
		}

	}
	
	public Service getNamedService(String name) {
		
		sm = ServiceManager.getInstance();
		for (Entry<String, ServiceUnit> entry : services.entrySet()) {
			if (name.equals(entry.getKey())) {
				return getNamedService(name, entry.getValue());
			}
		}
		return null;
	}
	
	public Service getNamedService(String name, ServiceUnit su) {
		return serviceProxy.getUniqueService(host.getIp(), su.getMonitorPort(), false);
	}
	
	public void updateService(String name, ServiceUnit su) {
		
		Service service = serviceProxy.getUniqueService(host.getIp(), su.getMonitorPort(), false);
		if (service != null) {
			
			if (sm.isRunning(name)) {
				service.setServiceStatus(ServiceStatus.RUNNING);
			} else {
				service.setServiceStatus(ServiceStatus.STOPED);
			}
			
			service.setZoneId(host.getAvailabilityZoneId());
			service.setClusterId(host.getClusterId());
			service.setUpdateTime(new Timestamp(System.currentTimeMillis()));
			
			logger.debug("update service" + service);
			serviceProxy.update(service);
		} else {
			logger.info("Could not find service with " + host.getIp() + ":" + su.getMonitorPort());
		}
	}
	
	public Map<String, ServiceUnit> getServices() {
		return services;
	}

	public void setServices(Map<String, ServiceUnit> services) {
		this.services = services;
	}

	public ServiceManager getSm() {
		return sm;
	}

	public void setSm(ServiceManager sm) {
		this.sm = sm;
	}

	public ServiceProxy getServiceProxy() {
		return serviceProxy;
	}

	public void setServiceProxy(ServiceProxy serviceProxy) {
		this.serviceProxy = serviceProxy;
	}

	public Host getHost() {
		return host;
	}

	public void setHost(Host host) {
		this.host = host;
	}

	public static void main(String [] args) {
		ServiceMonitor monitor = (ServiceMonitor) AppUtils.getApplicationContext().getBean("serviceMonitor");
		
		String name = "image-server";
		ServiceUnit su = new ServiceUnit();
		su.setMonitorPort(9009);
		
		monitor.updateService(name, su);
		
	}
}
