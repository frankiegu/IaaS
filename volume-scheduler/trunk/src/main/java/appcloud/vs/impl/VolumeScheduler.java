package appcloud.vs.impl;
import appcloud.common.constant.RoutingKeys;
import appcloud.common.exception.IllegalRpcArgumentException;
import appcloud.common.model.*;
import appcloud.common.model.VmImage.VmImageDiskFormatEnum;
import appcloud.common.model.VmImage.VmImageOSTypeEnum;
import appcloud.common.model.VmImage.VmImageStatusEnum;
import appcloud.common.model.VmSnapshot.VmSnapshotStatusEnum;
import appcloud.common.model.VmVolume.VmVolumeAttachStatusEnum;
import appcloud.common.model.VmVolume.VmVolumeStatusEnum;
import appcloud.common.model.VmVolume.VmVolumeTypeEnum;
import appcloud.common.model.VmVolume.VmVolumeUsageTypeEnum;
import appcloud.common.proxy.HostProxy;
import appcloud.common.proxy.VmVolumeProxy;
import appcloud.common.service.ImageServerService;
import appcloud.common.service.VolumeProviderService;
import appcloud.common.service.VolumeSchedulerService;
import appcloud.common.util.*;
import appcloud.rpc.tools.RpcException;
import appcloud.rpc.tools.RpcTimeoutException;
import appcloud.vs.Conf;
import appcloud.vs.VolumeSchedulerServer;
import appcloud.vs.strategy.SelectorService;
import appcloud.vs.util.Name2Class;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
/**
 * TODO consider thread safe
 * @author wangchao
 *
 */
public class VolumeScheduler implements VolumeSchedulerService{
	private static Logger logger = Logger.getLogger(VolumeScheduler.class);
	private static LolLogUtil loller = null;
	static {
		try {
			String ipAddress = VolumeSchedulerServer.getHost().getIp();
			loller = new LolLogUtil(MessageLog.ModuleEnum.VOLUME_SCHEDULER, VolumeScheduler.class, ipAddress);
		} catch (RpcException e) {
			logger.error(e.getMessage());
		}
	}
	private DBUtil db = DBUtil.getInstance();

	private ImageServerService is = null;
	private VolumeProviderService vp = null;
	
	public VolumeScheduler() {
		vp = (VolumeProviderService) ConnectionFactory.getAMQPService(
				VolumeProviderService.class, RoutingKeys.VOLUME_PROVIDER_PRE);
		is = (ImageServerService) ConnectionFactory.getAMQPService(ImageServerService.class,RoutingKeys.IMAGE_SERVER);
	}
	
	private List<Host> selectHost(VmVolume volume) throws Exception{
		Cluster cluster = db.getClusterById(volume.getAvailabilityClusterId());
		List<Host> hosts = selectHost(cluster, volume.getSize(), null, null);
		if(hosts.size() == 0){
			throw new RpcException("Volume-scheduler find zero host");
		}
		return hosts;
	}
	
	private Host selectOneHost(VmVolume volume, String exHostUuid) throws Exception{
		List<Host> hosts = selectHost(volume);
		if (hosts.size() == 0)
			throw new RpcException("Volume-scheduler find zero host");
		for (Host host : hosts) {
			if (!host.getUuid().equals(exHostUuid)) {
				return host;
			}
		}
		return null;
	}
	
	
	public List<Host> selectHost(Cluster cluster, int size, Host exhost, RpcExtra rpcExtra) throws RpcException {
		logger.info("cluster: "+cluster+", size:"+size+", exhost:"+exhost);
		if (cluster == null) {
			logger.info("cluster is null");
			loller.error(LolLogUtil.SELECT_HOST, "volume scheduler select host cluster is null", rpcExtra);
			throw new RpcException("olume scheduler select host cluster is null, so select host fail");
		}
		try {
			SelectorService hostSelector = Name2Class.getInstance(cluster.getId());
			List<Host> hosts = hostSelector.selectNodes(cluster, size, exhost);
			if (hosts != null && hosts.size() > 0) {
				logger.info(hosts);
				return hosts;// .get(0);
			} else {
				loller.error(LolLogUtil.SELECT_HOST, "Volume-scheduler no available volume-provider ", rpcExtra);
				throw new RpcException("Volume-scheduler no available volume-provider ");
			}
		} catch (Exception e) {
			loller.error(LolLogUtil.SELECT_HOST, "Volume-scheduler no available volume-provider, cann't connect to db ", rpcExtra);
			throw new RpcException("Volume-scheduler no available volume-provider,  cann't connect to db ");
		}

	}
	
	private String getRoutingKey(String uuid){
		return RoutingKeyGenerator.getRoutingKey(RoutingKeys.VOLUME_PROVIDER_PRE, uuid);
	}
	
	private VmVolume newVolume(){
		VmVolume volume = new VmVolume();
		String uuid = UuidUtil.getRandomUuid();
		volume.setVolumeUuid(uuid);
		volume.setName(uuid);
		volume.setDisplayName(uuid);
		volume.setDisplayDescription(uuid);
		volume.setStatus(VmVolumeStatusEnum.DEFINED);
		volume.setVolumeType(VmVolumeTypeEnum.QCOW2);
		return volume;
	}

	public VmVolume defineVolume(VmVolumeUsageTypeEnum usage, Integer userId, int size, VmZone zone, String imageUUID, Host host, RpcExtra rpcExtra)
			throws RpcException {
		System.out.println("**********************************************");
		logger.info("Starting defineVolume: " + userId +" " + size+" " + zone +" " + imageUUID +" "+ host);
		logger.info("Volume type: " +usage);
		//1.check
//		if(userId < 0 ){
//			loller.error(LolLogUtil.DEFINE_VOLUME, "params userId illegal, userId: "+ userId, rpcExtra);
//			throw new IllegalRpcArgumentException("Volume-scheduler defineVolue params illegal(userId < 0)");
//		}
//		if(zone == null || zone.getId() == null){
//			loller.error(LolLogUtil.DEFINE_VOLUME, "params userId illegal, zone: "+ zone, rpcExtra);
//			throw new IllegalRpcArgumentException("Volume-scheduler defineVolue params illegal(zone is null)");
//		}
//
//		if(size <0) {
//			loller.error(LolLogUtil.DEFINE_VOLUME, "params userId illegal, size: "+ size, rpcExtra);
//			throw new IllegalRpcArgumentException("Volume-scheduler defineVolue params illegal(size < 0");
//		}

		//2.before
		//3.do job
		VmVolume volume = newVolume();
		volume.setUsageType(usage);
		volume.setUserId(userId);
		volume.setSize(size);
		volume.setAvailabilityZoneId(zone.getId());
		volume.setAttachStatus(VmVolumeAttachStatusEnum.DETACHED);
		logger.info("init volume: "+ volume);
		
		if(imageUUID != null && !"".equals(imageUUID)){
			volume.setImageUuid(imageUUID);
			try {
				VmImage image = db.getImage(imageUUID);
				if(image.getDiskFormat().equals(VmImageDiskFormatEnum.ISO)){
					volume.setVolumeType(VmVolumeTypeEnum.ISO);
				}
			} catch (Exception e) {
				if(e.getMessage() != null && e.getMessage().startsWith("TIP Client")) {
					String why = "volume-scheduler connect to db-proxy failed!";
					logger.error(why);
//					loller.error(LolLogUtil.DEFINE_VOLUME, why, rpcExtra);
					throw new RpcException(why);
				} else {
					String why = "volume-scheduler invalid imageUUID" + imageUUID;
					logger.error(why, e);
//					loller.error(LolLogUtil.DEFINE_VOLUME, "invalid imageUUID"
//							+ imageUUID, rpcExtra);
					throw new RpcException(why);
				}
			}
			
		}
		if(host != null){
			volume.setHost(host);
			volume.setHostUuid(host.getUuid());
			volume.setAvailabilityClusterId(host.getClusterId());
		}
		//4.update
		try {
			db.save(volume);
			logger.info("Successfully define Volume " +volume);
//			loller.info(LolLogUtil.DEFINE_VOLUME, "Successfully define Volume " +volume, rpcExtra);
			return db.getVolume(volume.getVolumeUuid());
		} catch (Exception e) {
			logger.error("volume-scheduler save failed in db", e);
//			loller.error(LolLogUtil.DEFINE_VOLUME, "volume-scheduler save volume failed in db", rpcExtra);
			throw new RpcException("volume-scheduler save failed in db", e);
		}
	}

	public VmVolume defineVolumeOnImageBack(VmVolumeUsageTypeEnum usage, Integer userId, int size, VmZone zone, String imageUUID, Host host, RpcExtra rpcExtra)
			throws RpcException {
		logger.info("Starting defineVolume: " + userId +" " + size+" " + zone +" " + imageUUID +" "+ host);
		//1.check
		if(userId < 0 ){
			loller.error(LolLogUtil.DEFINE_VOLUME, "params userId illegal, userId: "+ userId, rpcExtra);
			throw new IllegalRpcArgumentException("Volume-scheduler defineVolue params illegal(userId < 0)");
		}
		if(zone == null || zone.getId() == null){
			loller.error(LolLogUtil.DEFINE_VOLUME, "params userId illegal, zone: "+ zone, rpcExtra);
			throw new IllegalRpcArgumentException("Volume-scheduler defineVolue params illegal(zone is null)");
		}

		if(size <0) {
			loller.error(LolLogUtil.DEFINE_VOLUME, "params userId illegal, size: "+ size, rpcExtra);
			throw new IllegalRpcArgumentException("Volume-scheduler defineVolue params illegal(size < 0");
		}

		//2.before
		//3.do job
		VmVolume volume = newVolume();
		volume.setUsageType(usage);
		volume.setUserId(userId);
		volume.setSize(size);
		volume.setAvailabilityZoneId(zone.getId());
		volume.setAttachStatus(VmVolumeAttachStatusEnum.DETACHED);

		volume.setVolumeType(VmVolumeTypeEnum.QCOW2);

		if(host != null){
			volume.setHost(host);
			volume.setHostUuid(host.getUuid());
			volume.setAvailabilityClusterId(host.getClusterId());
		}
		//4.update
		try {
			db.save(volume);
			logger.info("Successfully define Volume " +volume);
			loller.info(LolLogUtil.DEFINE_VOLUME, "Successfully define Volume " +volume, rpcExtra);
			return db.getVolume(volume.getVolumeUuid());
		} catch (Exception e) {
			logger.error("volume-scheduler save failed in db", e);
			loller.error(LolLogUtil.DEFINE_VOLUME, "volume-scheduler save volume failed in db", rpcExtra);
			throw new RpcException("volume-scheduler save failed in db", e);
		}
	}

	public VmImageBack defineVolumeImageBack(String volumeUuid,String displayName,String des, RpcExtra rpcExtra) throws RpcException {

		try {
			logger.info("the volumeUuid: "+volumeUuid+" displayName: "+displayName+" des: "+des);
			VmVolume vmVolume = db.getVolume(volumeUuid);
			logger.info(vmVolume);
			VmImageBack vmImageBack = newVmImageBack(vmVolume);
			vmImageBack.setDisplayName(displayName == null?vmImageBack.getVolumeUuid():displayName);
			logger.info(vmImageBack);
			db.save(vmImageBack);
			return vmImageBack;
		} catch (Exception e) {
			if(e.getMessage() != null && e.getMessage().startsWith("TIP Client")) {
				String why = "volume-scheduler connect to db-proxy failed!";
				logger.error(why);
				loller.error(LolLogUtil.DEFINE_VOLUME_IMAGEBACK, why, rpcExtra);
				throw new RpcException(why);
			} else {
				String why = "volume-scheduler db operation fail, volumeUuid" + volumeUuid;
				logger.error(why, e);
				loller.error(LolLogUtil.DEFINE_VOLUME_IMAGEBACK, "invalid volumeUuid"
						+ volumeUuid, rpcExtra);
				throw new RpcException(why);
			}
		}
	}

	private VmImageBack newVmImageBack(VmVolume vmVolume) {
		VmImageBack imageBack = new VmImageBack();
		String uuid = UuidUtil.getRandomUuid();
		imageBack.setVolumeUuid(uuid);
		imageBack.setImageStatus(VmImageBack.VmImageBackStatusTypeEnum.DEFINED);
		imageBack.setUserId(vmVolume.getUserId());
		imageBack.setAvailabilityZoneId(vmVolume.getAvailabilityZoneId());
		imageBack.setAvailabilityClusterId(vmVolume.getAvailabilityClusterId());
		imageBack.setActiveVolumeUuid(vmVolume.getVolumeUuid());
		imageBack.setInstanceUuid(vmVolume.getInstanceUuid());
		imageBack.setHostUuid(vmVolume.getHostUuid());
		imageBack.setVolumeSize(vmVolume.getSize());
		imageBack.setTop(false);
		imageBack.setBackUp(false);
		imageBack.setUsageType(vmVolume.getUsageType());
		imageBack.setVolumeType(vmVolume.getVolumeType());
		return imageBack;
	}

	private String getImageServiceHost(String imageUUID, RpcExtra rpcExtra) throws RpcException, Exception{
		logger.info("getDownloadImage...");
		VmImage image = is.getDownloadImage(imageUUID, rpcExtra);
		logger.info("getDownloadImage finished");
		
		List<Service> services = image.getServices();
		
		if (services.size() > 0) {
			return services.get(0).getHostIp() ;
		}
		else{
			loller.error(LolLogUtil.GET_IMAGE_SERVER, "no image server", rpcExtra);
			throw new RpcException("volume-scheduler no image Server");
		}
	}
	
	/*
	 * 跟据provider物理主机与镜像uuid获取合适的镜像目录
	 */
	private String getImageFilePath(String hostUUID, VmVolume volume, VmImage image) throws Exception {
		String finalImageUUID = null;
		String imageFilePath = image.getDirectory();
		String imageUUID = image.getUuid();
		logger.info(hostUUID+", "+imageUUID);
		List<VmVolume> vmVolumes = (List<VmVolume>) db.getVmvolumeByHostAndImage(hostUUID, imageUUID);
		int maxLength = -1;
		for (VmVolume vmVolume : vmVolumes) {
			if (maxLength < vmVolume.getImageUuid().length()) {
				maxLength = vmVolume.getImageUuid().length();
				finalImageUUID = vmVolume.getImageUuid();
			}
		}
		int count = 0;
		for (VmVolume vmVolume : vmVolumes) {
			if (vmVolume.getImageUuid().equals(finalImageUUID))
				count++;
		}
		logger.info(count);
		if (count > Conf.DEFAULT_MAX_SHARE_IMAGE_NUM) {
			if (finalImageUUID.contains(Conf.IMAGE_SPLIT_STRING)) {
				finalImageUUID += Conf.IMAGE_UUID_SUFFIX;
			} else {
				finalImageUUID += Conf.IMAGE_SPLIT_STRING + Conf.IMAGE_UUID_SUFFIX;
			}
		}
		if (finalImageUUID.contains(Conf.IMAGE_SPLIT_STRING)) {
			String[] splitFilePath = imageFilePath.split("[/.]");// images/a/b/uuid.img
			String [] splitImageUUID = finalImageUUID.split(Conf.IMAGE_SPLIT_STRING);
			//imageFilePath = imageFilePath.replace(splitFilePath[3], splitFilePath[3] + Conf.IMAGE_SPLIT_STRING + splitImageUUID[1]);
		}
		logger.info("finalImageUUID: " + finalImageUUID);
		logger.info("imageFilePath: " + imageFilePath);
		volume.setImageUuid(finalImageUUID);
		return imageFilePath;
	}
	/***
	 * create a real volume
	 * @param host
	 * @param volume
	 * @param image
	 * @return
	 * @throws Exception
	 */
	private void createVolume(Host host, VmVolume volume, VmImage image, RpcExtra rpcExtra) throws Exception {
		try {
			String routingkey = getRoutingKey(host.getUuid());
			String uuid = volume.getVolumeUuid();
			Float size = volume.getSize().floatValue();
			
			logger.info("creating volume on host " + host.getIp() + " volume: " + volume.getVolumeUuid() + " routingkey:" +routingkey + " uuid:" + uuid + " size:" + size);
			logger.info("image:" + image);
			
			String imageServerIP = null;
			String imagefilePath = null;
			if(image != null){
				imageServerIP = getImageServiceHost(image.getUuid(),rpcExtra);
				imagefilePath = getImageFilePath(host.getUuid(), volume, image);
				logger.info("image:"+ image +" " + imageServerIP+" " + imagefilePath);
			} else {
				logger.info("image is null, imageUUid from volume info is:"+volume.getImageUuid()+" volumeUuid is:"+volume.getVolumeUuid());
			}
			//XXX we may do retry here
			/**
			 * 判断镜像类型，如果是windows类型的镜像，则不使用增量快照的方式创建
			 * windows创建时采用复制整个母镜像的方式进行创建
			 */
			VmVolume v = null;
			if(image != null){
				String osType = image.getOsType().toString();
				logger.info("image type is : " + osType);
				if(osType.equals("windows")){
					v = vp.createWindowsVolume(routingkey, uuid, imageServerIP, imagefilePath , size, rpcExtra);
				} else {
					v = vp.createVolume(routingkey, uuid, imageServerIP, imagefilePath , size, rpcExtra);
				}
			} else {
				v = vp.createVolume(routingkey, uuid, imageServerIP, imagefilePath , size, rpcExtra);
			}


			volume.setProviderLocation(v.getProviderLocation());
			volume.setVolumeType(v.getVolumeType());
			volume.setHostUuid(host.getUuid());
			
			loller.info(LolLogUtil.CREATE_VOLUME, "create volume success! volume:" + volume, rpcExtra);
			//TODO cluster
//			volume.setAvailabilityClusterId(host.get);
			
		} catch (RpcException e) {
			if(e instanceof RpcTimeoutException) {
				logger.error("volume-scheduler no response from volume-provider!");
				loller.error(LolLogUtil.CREATE_VOLUME, "no response from volume-provider", rpcExtra);
				throw new RpcTimeoutException("volume-scheduler no response from volume-provider ");
			} else {
				logger.warn("failed to createVolume on host :"+host.getIp(), e);
				loller.error(LolLogUtil.CREATE_VOLUME, "failed to createVolume on host :"+host.getIp() +" "+ e.getMessage(), rpcExtra);
				throw new RpcException("volume-scheduler " + e.getMessage());
			}
		}
	}

	//TODO
	private void createVolumeByImageBack(Host host, VmVolume volume, VmImageBack imageBack, RpcExtra rpcExtra) throws Exception {
		try {
			String routingkey = getRoutingKey(host.getUuid());
			String uuid = volume.getVolumeUuid();
			Float size = volume.getSize().floatValue();

			logger.info("creating volume depend on imageback on host " + host.getIp() + " volume: " + volume.getVolumeUuid() + " routingkey:" +routingkey + " uuid:" + uuid + " size:" + size);
			logger.info("imageBack:" + imageBack);

			// TODO 这里很重要，imageBack的location是否带有ip
			String imageServerIP = host.getIp();
			String imagefilePath = imageBack.getProviderLocation();
			logger.info("the complete locatioin is: "+imagefilePath);
			String[] imagefileList=imagefilePath.split("/:");
			if (imagefileList.length!=2) {
				throw new RpcTimeoutException("the provider_location of image_backing is error");
			}
			imagefilePath = imagefileList[1];

//			if(image != null){
//				imageServerIP = getImageServiceHost(image.getUuid(),rpcExtra);
//				imagefilePath = getImageFilePath(host.getUuid(), volume, image);
//				logger.info("image:"+ image +" " + imageServerIP+" " + imagefilePath);
//			} else {
//				logger.info("image is null, imageUUid from volume info is:"+volume.getImageUuid()+" volumeUuid is:"+volume.getVolumeUuid());
//			}

			//XXX we may do retry here
			VmVolume v = vp.createVolume(routingkey, uuid, imageServerIP, imagefilePath , size, rpcExtra);
			volume.setProviderLocation(v.getProviderLocation());
			volume.setVolumeType(v.getVolumeType());
			volume.setHostUuid(host.getUuid());

			loller.info(LolLogUtil.CREATE_VOLUME, "create volume success! volume:" + volume, rpcExtra);
			//TODO cluster
//			volume.setAvailabilityClusterId(host.get);

		} catch (RpcException e) {
			if(e instanceof RpcTimeoutException) {
				logger.error("volume-scheduler no response from volume-provider!");
				loller.error(LolLogUtil.CREATE_VOLUME, "no response from volume-provider", rpcExtra);
				throw new RpcTimeoutException("volume-scheduler no response from volume-provider ");
			} else {
				logger.warn("failed to createVolume on host :"+host.getIp(), e);
				loller.error(LolLogUtil.CREATE_VOLUME, "failed to createVolume on host :"+host.getIp() +" "+ e.getMessage(), rpcExtra);
				throw new RpcException("volume-scheduler " + e.getMessage());
			}
		}
	}

	
	private void revertVolume(Host host, VmVolume volume, VmImage image, RpcExtra rpcExtra) throws Exception {
		
		try {
			String routingkey = getRoutingKey(host.getUuid());
			String uuid = volume.getVolumeUuid();
			Float size = volume.getSize().floatValue();
			
			logger.info("revert volume on host " + host.getIp() + " volume: " + volume.getVolumeUuid() + " routingkey:" +routingkey + " uuid:" + uuid + " size:" + size);
			logger.info("image:" + image);
			
			String imageServerIP = null;
			String imagefilePath = null;
			if(image !=null){
				imagefilePath = image.getDirectory();
				imageServerIP = getImageServiceHost(image.getUuid(),rpcExtra);
				logger.info("image:"+ image +" " + imageServerIP+" " + imagefilePath);
			}
			//XXX we may do retry here
			VmVolume v = vp.revertVolume(routingkey, uuid, imageServerIP,imagefilePath , size, rpcExtra);
			volume.setProviderLocation(v.getProviderLocation());
			volume.setVolumeType(v.getVolumeType());
			volume.setHostUuid(host.getUuid());
			
			loller.info(LolLogUtil.REVERT_VOLUME, "revert volume success! volume:" + volume, rpcExtra);
			//TODO cluster
//			volume.setAvailabilityClusterId(host.get);
			
		} catch (RpcException e) {
			if(e instanceof RpcTimeoutException) {
				logger.error("volume-scheduler no response from volume-provider!");
				loller.error(LolLogUtil.REVERT_VOLUME, "no response from volume-provider", rpcExtra);
				throw new RpcTimeoutException("volume-scheduler no response from volume-provider ");
			} else {
				logger.warn("failed to revertVolume on host :"+host, e);
				loller.error(LolLogUtil.REVERT_VOLUME, "failed to revertVolume on host :"+host + e.getMessage(), rpcExtra);
				throw e;
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public VmVolume createVolume(String volumeUUID, RpcExtra rpcExtra)
			throws RpcException {
		logger.info("Starting createVolume: " +volumeUUID);
		try {
			//1.check
			Thread.sleep(10000);
			VmVolume volume = db.getVolume(volumeUUID);
			logger.info("creating: " + volume);
			//2.before
			volume.setStatus(VmVolumeStatusEnum.CREATING);
			db.update(volume);
			
			//3.do job
			List<Host> hosts = selectHost(volume);
			boolean success = false;
			VmImage image = volume.getImage();
			//logger.info("imageType is :"+ image.getOsType());//此处image可能为空
			Host hostDefinde = volume.getHost();
			logger.info("hostDefinde :"+String.valueOf(hostDefinde)+" type is:"+volume.getVolumeType());
			if(hostDefinde != null) {
				try{
					if(volume.getVolumeType().equals(VmVolumeTypeEnum.ISO)){
						String ishost = getImageServiceHost(image.getUuid(), rpcExtra);
						VmVolume v = vp.downloadImg(getRoutingKey(hostDefinde.getUuid()), ishost, image.getDirectory(), false, rpcExtra);
						volume.setProviderLocation(v.getProviderLocation());
					} else{
						createVolume(hostDefinde, volume, image, rpcExtra);
					}
					success = true;
					
					
				} catch (RpcException e) {
					loller.error(LolLogUtil.CREATE_VOLUME, "failed to create on host" + hostDefinde, rpcExtra);
					logger.error("failed to create on host" + hostDefinde);
				}
			} else {
				// do create
				for (Host host : hosts) {
					try{
						if(volume.getVolumeType().equals(VmVolumeTypeEnum.ISO)){
							String ishost = getImageServiceHost(image.getUuid(), rpcExtra);
							VmVolume v = vp.downloadImg(getRoutingKey(host.getUuid()), ishost, image.getDirectory(), false, rpcExtra);
							volume.setProviderLocation(v.getProviderLocation());
						} else{
							createVolume(host, volume, image, rpcExtra);
						}
						success = true;
						break;
						
					} catch (RpcException e) {
						loller.error(LolLogUtil.CREATE_VOLUME, "failed to create on host" + host, rpcExtra);
						logger.error("failed to create on host" + host);
					}
				}
			}




			//4. update status
			if(success){
				volume.setStatus(VmVolumeStatusEnum.AVAILABLE);
				db.update(volume);
				logger.info("Successfully createVolume: " +volume);
				loller.info(LolLogUtil.CREATE_VOLUME, "Successfully createVolume: " +volume, rpcExtra);
				return db.getVolume(volumeUUID);
			} else{
				logger.warn("Failed to create volume :"+ volumeUUID);
				volume.setStatus(VmVolumeStatusEnum.ERROR);
				db.update(volume);
				loller.error(LolLogUtil.CREATE_VOLUME, "Failed to create volume :"+ volumeUUID, rpcExtra);
				throw new RpcException("failed to create VmVolume");
			}
		} catch (Exception e) {
			if(e instanceof RpcTimeoutException) {
				logger.error("volume-scheduler no response from volume-provider!");
				loller.error(LolLogUtil.CREATE_VOLUME, "no response from volume-provider", rpcExtra);
				throw new RpcTimeoutException("volume-scheduler no response from volume-provider ");
			} else {
				String why = "volume-scheduler failed to create Volume," + e.getClass();
				logger.error(why,e);
				loller.error(LolLogUtil.CREATE_VOLUME, "failed to create Volume" + e.getMessage(), rpcExtra);
				throw new RpcException(why,e);
			}
		}
		
	}

	/**
	 * {@inheritDoc}
	 */
	public VmVolume createDataVolume(String volumeUUID, RpcExtra rpcExtra)
			throws RpcException {
		logger.info("Starting create data volume: " +volumeUUID);
		VmVolume volume = newVolume();
		return volume;
//		try {
//			//1.check
//			VmVolume volume = db.getVolume(volumeUUID);
//			logger.info("creating: " + volume);
//			//2.before
//			volume.setStatus(VmVolumeStatusEnum.CREATING);
//			db.update(volume);
//
//			//3.do job
//			List<Host> hosts = selectHost(volume);
//			boolean success = false;
//			VmImage image = volume.getImage();
//			//logger.info("imageType is :"+ image.getOsType());//此处image可能为空
//			Host hostDefinde = volume.getHost();
//			logger.info("hostDefinde :"+String.valueOf(hostDefinde)+" type is:"+volume.getVolumeType());
//			if(hostDefinde != null) {
//				try{
//					if(volume.getVolumeType().equals(VmVolumeTypeEnum.ISO)){
//						String ishost = getImageServiceHost(image.getUuid(), rpcExtra);
//						VmVolume v = vp.downloadImg(getRoutingKey(hostDefinde.getUuid()), ishost, image.getDirectory(), false, rpcExtra);
//						volume.setProviderLocation(v.getProviderLocation());
//					} else{
//						createVolume(hostDefinde, volume, image, rpcExtra);
//					}
//					success = true;
//
//
//				} catch (RpcException e) {
//					loller.error(LolLogUtil.CREATE_VOLUME, "failed to create on host" + hostDefinde, rpcExtra);
//					logger.error("failed to create on host" + hostDefinde);
//				}
//			} else {
//				// do create
//				for (Host host : hosts) {
//					try{
//						if(volume.getVolumeType().equals(VmVolumeTypeEnum.ISO)){
//							String ishost = getImageServiceHost(image.getUuid(), rpcExtra);
//							VmVolume v = vp.downloadImg(getRoutingKey(host.getUuid()), ishost, image.getDirectory(), false, rpcExtra);
//							volume.setProviderLocation(v.getProviderLocation());
//						} else{
//							createVolume(host, volume, image, rpcExtra);
//						}
//						success = true;
//						break;
//
//					} catch (RpcException e) {
//						loller.error(LolLogUtil.CREATE_VOLUME, "failed to create on host" + host, rpcExtra);
//						logger.error("failed to create on host" + host);
//					}
//				}
//			}
//
//
//
//
//			//4. update status
//			if(success){
//				volume.setStatus(VmVolumeStatusEnum.AVAILABLE);
//				db.update(volume);
//				logger.info("Successfully createVolume: " +volume);
//				loller.info(LolLogUtil.CREATE_VOLUME, "Successfully createVolume: " +volume, rpcExtra);
//				return db.getVolume(volumeUUID);
//			} else{
//				logger.warn("Failed to create volume :"+ volumeUUID);
//				volume.setStatus(VmVolumeStatusEnum.ERROR);
//				db.update(volume);
//				loller.error(LolLogUtil.CREATE_VOLUME, "Failed to create volume :"+ volumeUUID, rpcExtra);
//				throw new RpcException("failed to create VmVolume");
//			}
//		} catch (Exception e) {
//			if(e instanceof RpcTimeoutException) {
//				logger.error("volume-scheduler no response from volume-provider!");
//				loller.error(LolLogUtil.CREATE_VOLUME, "no response from volume-provider", rpcExtra);
//				throw new RpcTimeoutException("volume-scheduler no response from volume-provider ");
//			} else {
//				String why = "volume-scheduler failed to create Volume," + e.getClass();
//				logger.error(why,e);
//				loller.error(LolLogUtil.CREATE_VOLUME, "failed to create Volume" + e.getMessage(), rpcExtra);
//				throw new RpcException(why,e);
//			}
//		}

	}

	public VmVolume createVolumeOnImageBack(String volumeUUID, VmImageBack imageBack, RpcExtra rpcExtra)
			throws RpcException {
		logger.info("Starting createVolume: " +volumeUUID);
		try {
			//1.check
			VmVolume volume = db.getVolume(volumeUUID);
			logger.info("creating: " + volume);
			//2.before
			volume.setStatus(VmVolumeStatusEnum.CREATING);
			db.update(volume);

			//3.do job
			boolean success = false;
			VmImage image = volume.getImage();
			Host hostDefinde = volume.getHost();
			logger.info("hostDefinde :"+String.valueOf(hostDefinde)+" type is:"+volume.getVolumeType());
			try{

				createVolumeByImageBack(hostDefinde, volume, imageBack, rpcExtra);

				success = true;


			} catch (RpcException e) {
				loller.error(LolLogUtil.CREATE_VOLUME, "failed to create on host" + hostDefinde, rpcExtra);
				logger.error("failed to create on host" + hostDefinde);
			}

			//4. update status
			if(success){
				volume.setStatus(VmVolumeStatusEnum.AVAILABLE);
				db.update(volume);
				logger.info("Successfully createVolume: " +volume);
				loller.info(LolLogUtil.CREATE_VOLUME, "Successfully createVolume: " +volume, rpcExtra);
				return db.getVolume(volumeUUID);
			} else{
				logger.warn("Failed to create volume :"+ volumeUUID);
				volume.setStatus(VmVolumeStatusEnum.ERROR);
				db.update(volume);
				loller.error(LolLogUtil.CREATE_VOLUME, "Failed to create volume :"+ volumeUUID, rpcExtra);
				throw new RpcException("failed to create VmVolume");
			}
		} catch (Exception e) {
			if(e instanceof RpcTimeoutException) {
				logger.error("volume-scheduler no response from volume-provider!");
				loller.error(LolLogUtil.CREATE_VOLUME, "no response from volume-provider", rpcExtra);
				throw new RpcTimeoutException("volume-scheduler no response from volume-provider ");
			} else {
				String why = "volume-scheduler failed to create Volume," +e.getClass();
				logger.error(why,e);
				loller.error(LolLogUtil.CREATE_VOLUME, "failed to create Volume" + e.getMessage(), rpcExtra);
				throw new RpcException(why,e);
			}
		}

	}

	public VmImageBack createVolumeImageBack(String volumeUUID, RpcExtra rpcExtra)
			throws RpcException {
		logger.info("Starting createVolumeImageBack: " +volumeUUID);
		try {
			//1.check
			VmImageBack imageBack = db.getImageBack(volumeUUID,false,false,false,true);
			logger.info("creating: " + imageBack);
			//2.before
			imageBack.setImageStatus(VmImageBack.VmImageBackStatusTypeEnum.CREATING);
			db.update(imageBack);

			//3.do job
			boolean success = false;
			Host host = imageBack.getHost();

			/**
			 * 解释：
			 * 1.activeUuid是将要生成的一个增量镜像，所以他的uuid应该是imageback数据库中之前define的那一条数据的新的uuid.
			 * 2.imageBackUuid是现在磁盘正在使用的镜像uuid，增量镜像操作之后，该镜像就会作为imageBack。
			 */
			String activeUuid = imageBack.getVolumeUuid();
			String imageBackUuid = imageBack.getActiveVolumeUuid();
			logger.info("host :"+String.valueOf(host)+" type is:"+imageBack.getVolumeType());

			VmImageBack result = null;
			if (host != null) {
				result = vp.createVolumeImageBack(getRoutingKey(host.getUuid()),imageBackUuid,activeUuid, Float.valueOf(imageBack.getVolumeSize()),rpcExtra);
			}

			if (result!=null && result.getProviderLocation() != null&&result.getProviderLocation().length() !=0) success = true;


			//4. update status(交换状态)
			if(success){
				VmImageBack newImageProduce = imageBack;
				String newLocation = result.getProviderLocation();
				String newVolumeUuid = newImageProduce.getVolumeUuid();


				VmVolume volumeImageActive = db.getVolume(imageBackUuid);
				VmImageBack imageBackPrev = db.getImageBack(imageBack.getInstanceUuid(),true);

				/**
				 * 修改新生成的镜像，和原来磁盘使用的镜像uuid和保存地址进行交换
				 */
				newImageProduce.setImageStatus(VmImageBack.VmImageBackStatusTypeEnum.AVALIABLE);
				newImageProduce.setVolumeUuid(volumeImageActive.getVolumeUuid());
				newImageProduce.setProviderLocation(volumeImageActive.getProviderLocation());
				newImageProduce.setActiveVolumeUuid(volumeImageActive.getImageUuid());
				newImageProduce.setParentVolumeUuid(newVolumeUuid);
				newImageProduce.setTop(true);

				if (imageBackPrev != null) {
					newImageProduce.setSonVolumeUuid(imageBackPrev.getVolumeUuid());
					imageBackPrev.setTop(false);
					imageBackPrev.setParentVolumeUuid(newImageProduce.getVolumeUuid());
					db.update(imageBackPrev);
				}
				volumeImageActive.setBackupVolumeUuid(volumeImageActive.getVolumeUuid());
				volumeImageActive.setVolumeUuid(newVolumeUuid);
				volumeImageActive.setProviderLocation(newLocation);

				db.update(newImageProduce);
				db.update(volumeImageActive);
				logger.info("Successfully createVolume imageBack: " +newImageProduce);
				loller.info(LolLogUtil.CREATE_VOLUME_IMAGEBACK, "Successfully createVolume imageBack: " +newImageProduce, rpcExtra);
				return newImageProduce;
			} else{
				logger.warn("Failed to create volume imageBack:"+ volumeUUID);
				imageBack.setImageStatus(VmImageBack.VmImageBackStatusTypeEnum.ERROR);
				db.update(imageBack);
				loller.error(LolLogUtil.CREATE_VOLUME_IMAGEBACK, "Failed to create volume :"+ volumeUUID, rpcExtra);
				throw new RpcException("failed to create VmVolume");
			}
		} catch (Exception e) {
			if(e instanceof RpcTimeoutException) {
				logger.error("volume-scheduler no response from volume-provider!");
				loller.error(LolLogUtil.CREATE_VOLUME_IMAGEBACK, "no response from volume-provider", rpcExtra);
				throw new RpcTimeoutException("volume-scheduler no response from volume-provider ");
			} else {
				String why = "volume-scheduler failed to create Volume imageBack";
				logger.error(why,e);
				loller.error(LolLogUtil.CREATE_VOLUME_IMAGEBACK, "failed to create Volume imageBack" + e.getMessage(), rpcExtra);
				throw new RpcException(why,e);
			}
		}

	}


	/**
	 * {@inheritDoc}
	 */
	public VmVolume deleteVolume(String volumeUUID, RpcExtra rpcExtra) throws RpcException {
		logger.info("Starting deleteVolume: " +volumeUUID);
		VmVolume volume = null;
		try {
			//1.check
			volume = db.getVolume(volumeUUID);
			//2.before
			//3.do job
			//4.update status
			volume.setStatus(VmVolumeStatusEnum.DELETED);
			volume.setDeletedTime(new Timestamp(System.currentTimeMillis()));
			db.update(volume);
			//update status for snapshots
			List<? extends VmSnapshot> vmSnapshots = db.getSnapshots(volume);
			for(VmSnapshot at:vmSnapshots) {
				at.setStatus(VmSnapshotStatusEnum.DELETED);
				db.update(at);		
				logger.info("Successfully deleteSnapshot :"+ at.getName());
				loller.info(LolLogUtil.DELETE_VOLUME, "Successfully deleteSnapshot :"+ at.getName(), rpcExtra);
			}
			
			logger.info("Successfully deleteVolume :"+ volumeUUID);
			loller.info(LolLogUtil.DELETE_VOLUME, "Successfully deleteVolume :"+ volumeUUID, rpcExtra);
			return db.getVolume(volumeUUID);
		} catch (Exception e) {
			if(e.getMessage() != null && e.getMessage().startsWith("TIP Client")) {
				loller.error(LolLogUtil.DELETE_VOLUME, "connect to db-proxy failed", rpcExtra);
				throw new RpcException("volume-scheduler delete volume connect to db-proxy failed");
			} else {
				logger.warn(e);
				loller.error(LolLogUtil.DELETE_VOLUME, "delete volume failed! Cause by" + e.getMessage(), rpcExtra);
				throw new RpcException("volume-scheduler failed to operate db",e);
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void destroyVolume(String volumeUUID, RpcExtra rpcExtra) throws RpcException {
		logger.info("Starting destroyVolume: " +volumeUUID);
		VmVolume volume = null;
		try {
			//1.check
			volume = db.getVolume(volumeUUID);
			//2.before
			volume.setStatus(VmVolumeStatusEnum.DELETED);
			db.update(volume);
			//3.do job
			if(volume.getVolumeType() != VmVolumeTypeEnum.ISO){
				vp.deleteVolume(getRoutingKey(volume.getHostUuid()), volume.getVolumeUuid(), rpcExtra);
			}
			//4.update status
			db.deleteVolume(volume.getId());
			//destroy snapshots
			List<? extends VmSnapshot> vmSnapshots = db.getSnapshots(volume);
			for(VmSnapshot at:vmSnapshots) {
				db.deleteSnapshot(at.getId());
				logger.info("Successfully destroySnapshot :"+ at.getName());
				loller.info(LolLogUtil.DESTROY_VOLUME, "Successfully destroySnapshot :"+ at.getName(), rpcExtra);
			}
			
			logger.info("Successfully destroyVolume :"+ volumeUUID);
			loller.info(LolLogUtil.DESTROY_VOLUME, "Successfully destroyVolume :"+ volumeUUID, rpcExtra);
		} catch (Exception e) {
			if (e instanceof RpcTimeoutException) {
				logger.error("volume-scheduler no response from volume-provider");
				loller.error(LolLogUtil.DESTROY_VOLUME, "volume-scheduler no response from volume-provider", rpcExtra);
				throw new RpcTimeoutException("volume-scheduler no response from volume-provider");
			} else {
				logger.warn(e);
				loller.error(LolLogUtil.DESTROY_VOLUME, "failed to operate db"+e.getMessage(), rpcExtra);
				throw new RpcException("volume-scheduler failed to operate db",e);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void destroyVolumeImageBack(String volumeUUID, RpcExtra rpcExtra) throws RpcException {
		logger.info("Starting destroyVolume: " +volumeUUID);
		VmImageBack volume = null;
		try {
			//1.check
			volume = db.getImageBack(volumeUUID);
			//2.before
			volume.setImageStatus(VmImageBack.VmImageBackStatusTypeEnum.DELETED);
			db.update(volume);
			//3.do job
			if(volume.getVolumeType() != VmVolumeTypeEnum.ISO){
				vp.deleteVolume(getRoutingKey(volume.getHostUuid()), volume.getVolumeUuid(), rpcExtra);
			}
			//4.update status
			db.deleteImageBack(volume.getId());
			//destroy snapshots
			List<? extends VmSnapshot> vmSnapshots = db.getSnapshots(volume);
			if (vmSnapshots != null) {
				for (VmSnapshot at : vmSnapshots) {
					db.deleteSnapshot(at.getId());
					logger.info("Successfully destroySnapshot :" + at.getName());
					loller.info(LolLogUtil.DEFINE_VOLUME_IMAGEBACK, "Successfully destroySnapshot :" + at.getName(), rpcExtra);
				}
			}
			logger.info("Successfully destroyVolume :"+ volumeUUID);
			loller.info(LolLogUtil.DEFINE_VOLUME_IMAGEBACK, "Successfully destroyVolume :"+ volumeUUID, rpcExtra);
		} catch (Exception e) {
			if (e instanceof RpcTimeoutException) {
				logger.error("volume-scheduler no response from volume-provider");
				loller.error(LolLogUtil.DEFINE_VOLUME_IMAGEBACK, "volume-scheduler no response from volume-provider", rpcExtra);
				throw new RpcTimeoutException("volume-scheduler no response from volume-provider");
			} else {
				logger.warn(e);
				loller.error(LolLogUtil.DEFINE_VOLUME_IMAGEBACK, "failed to operate db"+e.getMessage(), rpcExtra);
				throw new RpcException("volume-scheduler failed to operate db",e);
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public VmVolume resizeVolume(String volumeUUID, Integer size, RpcExtra rpcExtra) throws RpcException {
		logger.info("Starting resizeVolume: " +volumeUUID +" size "+size);
		try {
			//1.check
			VmVolume volume = db.getVolume(volumeUUID);
			if(!volume.getStatus().equals(VmVolumeStatusEnum.AVAILABLE)){
				loller.error(LolLogUtil.RESIZE_VOLUME, "cannot resize an unAvailable volume:"+volumeUUID, rpcExtra);
				throw new RpcException("volume-scheduler cannot resize an unAvailable volume");
			}
			//2.before
			//TODO resizing 状态
			
			//3.do job
			volume = vp.resizeRawImg(getRoutingKey(volume.getHostUuid()), volume.getVolumeUuid(), size.floatValue(), rpcExtra);
			//4.update status
			volume.setSize(size);
			db.update(volume);			
			logger.info("Successfully resizeVolume :"+ volumeUUID);
			loller.info(LolLogUtil.RESIZE_VOLUME, "Successfully resizeVolume :"+ volumeUUID, rpcExtra);
			return volume;
		} catch (RpcException e) {
			if (e instanceof RpcTimeoutException) {
				logger.error("volume-scheduler no response from volume-provider");
				loller.error(LolLogUtil.RESIZE_VOLUME, "volume-scheduler no response from volume-provider", rpcExtra);
				throw new RpcTimeoutException("volume-scheduler no response from volume-provider");
			}
			String why = "volume-scheduler failed to resize"; 
			logger.warn(why, e);
			loller.error(LolLogUtil.RESIZE_VOLUME, why+e.getMessage(), rpcExtra);
			throw new RpcException(why, e);
		} catch (Exception e) {
			String why = "volume-scheduler failed to resize in db";
			logger.warn(why, e);
			loller.error(LolLogUtil.RESIZE_VOLUME, why+e.getMessage(), rpcExtra);
			throw new RpcException(why, e);
		}
		
	}
	
	private String getSrcImageUUID(String imageUUID) {
		if (imageUUID.contains(Conf.IMAGE_SPLIT_STRING)) {
			String[] splitUUID = imageUUID.split(Conf.IMAGE_SPLIT_STRING);
			return splitUUID[0];
		}
		return imageUUID;
	}
	
	private String getRealDirectory(String srcUUID, String filePath) {
		if (srcUUID.contains(Conf.IMAGE_SPLIT_STRING)) {
			String[] splitFilePath = filePath.split("[/.]");// images/a/b/uuid.img
			String [] splitImageUUID = srcUUID.split(Conf.IMAGE_SPLIT_STRING);
			filePath = filePath.replace(splitFilePath[3], splitFilePath[3] + Conf.IMAGE_SPLIT_STRING + splitImageUUID[1]);
		}
		return filePath;
	}
	/**
	 * {@inheritDoc}
	 */
	public VmVolume cloneVolume(String srcUUID, String destUUID, RpcExtra rpcExtra) throws RpcException {
		logger.info("Starting cloneVolume, src: " +srcUUID + " dest: "+destUUID);
		try {
			//1.check 
			VmVolume src = db.getVolume(srcUUID);
			VmVolume dest = db.getVolume(destUUID);
			
			if(!src.getStatus().equals(VmVolumeStatusEnum.AVAILABLE) ||
				!dest.getStatus().equals(VmVolumeStatusEnum.DEFINED)){
				loller.error(LolLogUtil.CLONE_VOLUME, "invalid status, volume:"+ src + " "+ dest, rpcExtra);
				throw new RpcException("volume-scheduler invalid status");
			}
			
			//2.before
			dest.setStatus(VmVolumeStatusEnum.CREATING);
			db.update(dest);
			//3.do job
			String routingkey = getRoutingKey(src.getHostUuid());
			
			Host host = selectOneHost(dest, src.getHostUuid());
			if (host == null) {
				String why = "volumescheduler select host fail!";
				logger.info(why);
				loller.error(LolLogUtil.CLONE_VOLUME, why);
				throw new RpcException(why);
			}
			dest.setHostUuid(host.getUuid());
			dest.setHost(host);
			String destHost = host.getIp();
			logger.info("Backup destHost: "+destHost);
			
			String imageDir = null;
			
			if(src.getImageUuid() != null && !"".equals(src.getImageUuid())){
				dest.setImageUuid(src.getImageUuid());
				String srcImageUUID = getSrcImageUUID(src.getImageUuid());
				VmImage vmImage = db.getImage(srcImageUUID);
				if(vmImage != null) {
					imageDir = getRealDirectory(src.getImageUuid(), vmImage.getDirectory());
				} else {
					loller.error(LolLogUtil.CLONE_VOLUME, "not found image by imageuuid:"+src.getImageUuid(), rpcExtra);
					throw new RpcException("volume-scheduler not found image by imageuuid:"+src.getImageUuid());
				}
			}
			
			try {
				VmVolume v =vp.copyImg(routingkey, srcUUID, destHost, destUUID, imageDir, rpcExtra);
				dest.setProviderLocation(v.getProviderLocation());
				
			} catch (Exception e) {
				logger.error("failed to clone volume ",e);
				loller.error(LolLogUtil.CLONE_VOLUME, "failed to clone volume "+e.getMessage(), rpcExtra);
				vp.deleteVolume(getRoutingKey(dest.getHostUuid()), dest.getVolumeUuid(), rpcExtra);				
				throw e;
			}
			
			//TODO test
			logger.info("cloning snapshots");
			List<? extends VmSnapshot> snapshots = db.getSnapshots(src);
			for (VmSnapshot vs : snapshots) {
				vs.setVmVolume(dest);
				vs.setVolumeId(dest.getId());
				vs.setVolumeUuid(dest.getVolumeUuid());
				vs.setId(null);
				db.save(vs);
				logger.info("cloned snapshot: "+vs);
			}
			logger.info("cloned snapshots");
			
			//4.update status
			dest.setVolumeType(src.getVolumeType());
			dest.setUsageType(src.getUsageType());
			dest.setBackupVolumeUuid(srcUUID);
			dest.setStatus(VmVolumeStatusEnum.AVAILABLE);
			db.update(dest);
			logger.info("Successfully cloneVolume, src: " +srcUUID + " dest: "+destUUID);
			loller.info(LolLogUtil.CLONE_VOLUME, "Successfully cloneVolume, src: " +srcUUID + " dest: "+destUUID, rpcExtra);
			return db.getVolume(destUUID);
		} catch (RpcException e) {
			if (e instanceof RpcTimeoutException) {
				logger.error("volume-scheduler no response from volume-provider");
				loller.error(LolLogUtil.CLONE_VOLUME, "volume-scheduler no response from volume-provider", rpcExtra);
				throw new RpcTimeoutException("volume-scheduler no response from volume-provider");
			}
			logger.error("failed to clone volume ",e);
			loller.error(LolLogUtil.CLONE_VOLUME, "failed to clone volume " + e.getMessage(), rpcExtra);
			throw e;
		} catch (Exception e) {
			String why = "failed to clone volume in db";
			logger.error(why,e);
			loller.error(LolLogUtil.CLONE_VOLUME, why+e.getMessage(), rpcExtra);
			throw new RpcException(why,e);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public VmVolume moveVolume(String volumeUUID, Host host, RpcExtra rpcExtra) throws RpcException {
		logger.info("Starting moveVolume: " +volumeUUID + " to host: "+host);
		try {
			//1.check
			VmVolume volume = db.getVolume(volumeUUID);
			if(!volume.getStatus().equals(VmVolumeStatusEnum.AVAILABLE)){
				loller.error(LolLogUtil.MOVE_VOLUME, "invalid status, volumeUUID: "+volumeUUID, rpcExtra);
				throw new RpcException("invalid status"); 
			}
			//2.before
			volume.setStatus(VmVolumeStatusEnum.CREATING);
			db.update(volume);
			
			//3.do job
			String routingkey = getRoutingKey(volume.getHostUuid());
			
			if(volume.getHost().getIp().equals(host.getIp())){
				logger.warn("moveVolume to same host ");
				loller.warn(LolLogUtil.MOVE_VOLUME, "moveVolume to same host ", rpcExtra);
			} else {
				String imageuuid = volume.getImageUuid();
				String imagepath = null;
				if(imageuuid != null){
					imagepath = db.getImage(imageuuid).getDirectory();
				}
				//XXX make following two actions atomic
				VmVolume v = vp.copyImg(routingkey, volumeUUID, host.getIp(), volumeUUID,imagepath,rpcExtra);
				volume.setProviderLocation(v.getProviderLocation());
				vp.deleteVolume(routingkey, volumeUUID,rpcExtra);
			}
			
			//4.update status
			volume.setHost(host);
			volume.setHostUuid(host.getUuid());
			
			db.update(volume);
			logger.info("Successfully moveVolume: " +volumeUUID + " to host: "+host);
			loller.info(LolLogUtil.MOVE_VOLUME, "Successfully moveVolume: " +volumeUUID + " to host: "+host, rpcExtra);
			return db.getVolume(volumeUUID);
		}  catch (RpcException e) {
			if (e instanceof RpcTimeoutException) {
				String why = "volume-scheduler no response from volume-provider";
				logger.error(why);
				loller.error(LolLogUtil.MOVE_VOLUME, why, rpcExtra);
				throw new RpcTimeoutException(why);
			}
			logger.error("volume-schedule failed to move volume ",e);
			loller.error(LolLogUtil.MOVE_VOLUME, "volume-schedule failed to move volume "+e.getMessage(), rpcExtra);
			throw new RpcException("volume-schedule failed to move volume ");
		} catch (Exception e) {
			String why = "volume-schedule failed to move Volume in db";
			logger.error(why,e);
			loller.error(LolLogUtil.MOVE_VOLUME, why+e.getMessage(), rpcExtra);
			throw new RpcException(why,e);
		}
		
	}
	private VmSnapshot newSnapshot(){
		VmSnapshot vs = new VmSnapshot();
		vs.setUuid(UuidUtil.getRandomUuid());
		vs.setCreatedTime(new Timestamp(System.currentTimeMillis()));
		return vs;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public VmSnapshot defineSnapshot(String volumeUUID, String name,String displayDescription, RpcExtra rpcExtra)
			throws RpcException {
		logger.info("Starting defineSnapshot: " +volumeUUID + " snapshot: "+name);
		try {
			//1.check
			
			VmVolume volume = db.getVolume(volumeUUID);
			//2.before
			//3.do job
			//4.update status
			VmSnapshot vs = newSnapshot();
			vs.setName(name);
			vs.setUserId(volume.getUserId());
			vs.setDisplayName(name);
			vs.setDisplayDescription(displayDescription);
			
			vs.setVmVolume(volume);
			vs.setVolumeId(volume.getId());
			vs.setVolumeUuid(volumeUUID);
			vs.setVolumeSize(volume.getSize());
			vs.setStatus(VmSnapshotStatusEnum.DEFINED);
			db.save(vs);
			
			logger.info("Successfully defineSnapshot: " +volumeUUID + " snapshot: "+name);
			loller.info(LolLogUtil.DEFINE_SNAPSHOT, "Successfully defineSnapshot: " +volumeUUID + " snapshot: "+name, rpcExtra);
			return db.getSnapshot(vs.getUuid());
			
		} catch (Exception e) {
			String why = "failed to defineSnashot in db";
			logger.error(why,e);
			loller.error(LolLogUtil.DEFINE_SNAPSHOT, why+e.getMessage(), rpcExtra);
			throw new RpcException(why,e);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public VmSnapshot createSnapshot(String volumeUUID, Integer snapshotId, RpcExtra rpcExtra)
			throws RpcException {
		logger.info("Starting createSnapshot: " +volumeUUID + " snapshot: "+snapshotId);
		try {
			//1.check
			
			VmSnapshot snapshot = db.getSnapshot(snapshotId);
			VmVolume volume = snapshot.getVmVolume();
			logger.info("snapshot:" + snapshot +" volume:" + volume);
			if(!snapshot.getStatus().equals(VmSnapshotStatusEnum.DEFINED)){
				loller.error(LolLogUtil.CREATE_SNAPSHOT, "invalid status of snapshot:"+ snapshot, rpcExtra);
				throw new RpcException("volume-scheduler invalid status");
			}
			if(!snapshot.getVolumeUuid().equals(volumeUUID)){
				String why = "volume-scheduler invalid volume & snapshot";
				logger.error(why);
				loller.error(LolLogUtil.CREATE_SNAPSHOT, why + " " + snapshot, rpcExtra);
				throw new RpcException(why);
			}
			
			
			//2.before
			snapshot.setStatus(VmSnapshotStatusEnum.CREATING);
			db.update(snapshot);
			//3.do job
			vp.createSnapshot(getRoutingKey(volume.getHostUuid()), volumeUUID, snapshot.getUuid(),rpcExtra);
			//4.update status
			snapshot.setStatus(VmSnapshotStatusEnum.AVAILABLE);
			db.update(snapshot);
			logger.info("Successfully createSnapshot: " +volumeUUID + " snapshot: "+snapshotId);
			loller.info(LolLogUtil.CREATE_SNAPSHOT, "Successfully createSnapshot: " +volumeUUID + " snapshot: "+snapshotId, rpcExtra);
			return db.getSnapshot(snapshotId);
		} catch (RpcException e) {
			if (e instanceof RpcTimeoutException) {
				String why = "volume-scheduler no response from volume-provider";
				logger.error(why);
				loller.error(LolLogUtil.CREATE_SNAPSHOT, why, rpcExtra);
				throw new RpcTimeoutException(why);
			}
			logger.error("volume-schedule failed to create snapshot ",e);
			loller.error(LolLogUtil.CREATE_SNAPSHOT, "volume-schedule failed to create snapshot "+e.getMessage(), rpcExtra);
			throw new RpcException("volume-schedule failed to create snapshot ");
		} catch (Exception e) {
			String why = "volume-schedule failed to create snapshot in db";
			logger.error(why, e);
			loller.error(LolLogUtil.CREATE_SNAPSHOT, why+e.getMessage(), rpcExtra);
			throw new RpcException(why, e);
		}
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public VmSnapshot deleteSnapshot(String volumeUUID, Integer snapshotId, RpcExtra rpcExtra)
			throws RpcException {
		logger.info("Starting deleteSnapshot: " +volumeUUID + " snapshot: "+snapshotId);
		try {
			//1.check
			VmSnapshot snapshot = db.getSnapshot(snapshotId);
			VmVolume volume = snapshot.getVmVolume();
			if(!snapshot.getVolumeUuid().equals(volumeUUID)){
				String why = "invalid volume & snapshot";
				logger.error(why);
				loller.error(LolLogUtil.DELETE_SNAPSHOT, why+" "+snapshot, rpcExtra);
				throw new RpcException(why);
			}
			
			//2.before
			snapshot.setStatus(VmSnapshotStatusEnum.DELETING);
			db.update(snapshot);
			//3.do job
			vp.deleteSnapshot(getRoutingKey(volume.getHostUuid()), volumeUUID, snapshot.getUuid(), rpcExtra);
			//4.update status
			snapshot.setDeletedTime(new Timestamp(System.currentTimeMillis()));
			snapshot.setStatus(VmSnapshotStatusEnum.DELETED);
			db.update(snapshot);
			//TODO delete the snapshot from database
			logger.info("Successfully deleteSnapshot: " +volumeUUID + " snapshot: "+snapshotId);
			loller.info(LolLogUtil.DELETE_SNAPSHOT, "Successfully deleteSnapshot: " +volumeUUID + " snapshot: "+snapshotId, rpcExtra);
			return db.getSnapshot(snapshotId);
		} catch (RpcException e) {
			if (e instanceof RpcTimeoutException) {
				String why = "volume-scheduler no response from volume-provider";
				logger.error(why);
				loller.error(LolLogUtil.DELETE_SNAPSHOT, why, rpcExtra);
				throw new RpcTimeoutException(why);
			}
			logger.error("volume-schedule failed to delete snapshot ",e);
			loller.error(LolLogUtil.DELETE_SNAPSHOT, "volume-schedule failed to  delete snapshot "+e.getMessage(), rpcExtra);
			throw new RpcException("volume-schedule failed to  delete snapshot ");
		} catch (Exception e) {
			String why = "volume-schedule failed to  delete snapshot in db";
			logger.error(why,e);
			loller.error(LolLogUtil.DELETE_SNAPSHOT, why+e.getMessage(), rpcExtra);
			throw new RpcException(why,e);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public VmSnapshot applySnapshot(String volumeUUID, Integer snapshotId, RpcExtra rpcExtra)
			throws RpcException {
		logger.info("Starting applySnapshot: " +volumeUUID + " snapshot: "+snapshotId);
		try {
			//1.check
			VmSnapshot snapshot = db.getSnapshot(snapshotId);
			VmVolume volume = snapshot.getVmVolume();
			if(!snapshot.getVolumeUuid().equals(volumeUUID)){
				String why = "volume-scheduler invalid volume & snapshot";
				logger.error(why);
				loller.error(LolLogUtil.APPLY_SNAPSHOT, why+" "+snapshot, rpcExtra);
				throw new RpcException(why);
			}
			if(snapshot.getStatus() != VmSnapshotStatusEnum.AVAILABLE){
				String why ="volume-scheduler invalid snapshot status";
				logger.error(why);
				loller.error(LolLogUtil.APPLY_SNAPSHOT, why+" "+snapshot, rpcExtra);
				throw new RpcException(why);
			}
			//2.before
			//XXX APPLYING
			//3.do job
			vp.applySnapshot(getRoutingKey(volume.getHostUuid()), volumeUUID, snapshot.getUuid(), rpcExtra);
			//4.update status
			logger.info("Successfully applySnapshot: " +volumeUUID + " snapshot: "+snapshotId);
			loller.info(LolLogUtil.APPLY_SNAPSHOT, "Successfully applySnapshot: " +volumeUUID + " snapshot: "+snapshotId, rpcExtra);
			return snapshot;
		} catch (RpcException e) {
			if (e instanceof RpcTimeoutException) {
				String why = "volume-scheduler no response from volume-provider";
				logger.error(why);
				loller.error(LolLogUtil.APPLY_SNAPSHOT, why, rpcExtra);
				throw new RpcTimeoutException(why);
			}
			logger.error("volume-schedule applySnapshot failed ",e);
			loller.error(LolLogUtil.APPLY_SNAPSHOT, "volume-schedule applySnapshot failed "+e.getMessage(), rpcExtra);
			throw new RpcException("volume-schedule applySnapshot failed ");
		} catch (Exception e) {
			String why = "volume-schedule applySnapshot failed in db";
			logger.error(why,e);
			loller.error(LolLogUtil.APPLY_SNAPSHOT, why+e.getMessage(), rpcExtra);
			throw new RpcException(why,e);
		} 
	}	
	/**
	 * {@inheritDoc}
	 */
	public VmVolume convertImgFormat(String volumeUUID, VmImage image, final RpcExtra rpcExtra) 
			throws RpcException {
		String routingkey = "";
		try {
			final VmVolume volume = db.getVolume(volumeUUID);
			routingkey = getRoutingKey(volume.getHostUuid());
			logger.info("convert image routingkey : " + routingkey);
			if(volume.getStatus() != VmVolumeStatusEnum.AVAILABLE){
				String why ="invalid volume status";
				logger.error(why);
				loller.error(LolLogUtil.CONVERT_IMG_FORMAT, why+"volume:"+volume, rpcExtra);
				throw new RpcException(why);
			}
			//rebase
			logger.info("cloned old volume");
			return vp.convertImgFormat(routingkey, volume.getVolumeType().toString(), volume.getVolumeUuid(), image.getUuid(), rpcExtra);
		} catch (RpcException e) {
			if (e instanceof RpcTimeoutException) {
				String why = "volume-scheduler no response from volume-provider";
				logger.error(why);
				loller.error(LolLogUtil.CONVERT_IMG_FORMAT, why, rpcExtra);
				throw new RpcTimeoutException(why);
			}
			String why ="volume-scheduler convert image format failed" + image.getUuid();
			logger.warn(why,e);
			vp.deleteVolume(routingkey, image.getUuid(), rpcExtra);
			loller.error(LolLogUtil.CONVERT_IMG_FORMAT, why+e.getMessage(), rpcExtra);
			throw new RpcException(why,e);
		} catch (Exception e){
			String why ="volume-scheduler imageProxy failed, image: " + image;
			logger.warn(why,e);
			vp.deleteVolume(routingkey, image.getUuid(), rpcExtra);
			loller.error(LolLogUtil.CONVERT_IMG_FORMAT, why+e.getMessage(), rpcExtra);
			throw new RpcException(why,e);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public VmImage uploadImage(String volumeUUID, VmImage image, final RpcExtra rpcExtra) throws RpcException {
		String routingkey = "";
		try {
			final VmVolume volume = db.getVolume(volumeUUID);
			routingkey = getRoutingKey(volume.getHostUuid());
			
			if(volume.getStatus() != VmVolumeStatusEnum.AVAILABLE){
				String why ="invalid volume status";
				logger.error(why);
				loller.error(LolLogUtil.UPLOAD_IMAGE, why+"volume:"+volume, rpcExtra);
				throw new RpcException(why);
			}
			//copy image to servers
			String destPath = NfsLocationTool.getNfsLocation(image.getMd5sum()+".img");
			for (Service service : image.getServices()) {
				logger.info("do upload to service: " + service);
				logger.info("upload to image server : " + destPath);
				vp.releaseImg(routingkey, image.getUuid(), service.getHostIp(), destPath, rpcExtra);
			}
			logger.info("finished upload!");
			
			return image;
		} catch (RpcException e) {
			if (e instanceof RpcTimeoutException) {
				String why = "volume-scheduler no response from volume-provider";
				logger.error(why);
				loller.error(LolLogUtil.UPLOAD_IMAGE, why, rpcExtra);
				throw new RpcTimeoutException(why);
			}
			String why ="volume-scheduler upload image failed" + image.getUuid();
			logger.warn(why,e);
			vp.deleteVolume(routingkey, image.getUuid(), rpcExtra);
			loller.error(LolLogUtil.UPLOAD_IMAGE, why+e.getMessage(), rpcExtra);
			throw new RpcException(why,e);
		}catch (Exception e){
			String why ="volume-scheduler imageProxy failed, image: " + image;
			logger.warn(why,e);
			vp.deleteVolume(routingkey, image.getUuid(), rpcExtra);
			loller.error(LolLogUtil.UPLOAD_IMAGE, why+e.getMessage(), rpcExtra);
			throw new RpcException(why,e);
		}
	}

	public VmImage authorizeImage(String sourcePath, VmImage image, final RpcExtra rpcExtra) throws RpcException {
		String routingkey = "";
//		String destPath = image.getDirectory();
		String destPath = NfsLocationTool.getNfsLocation(image.getUuid()+".img");
		try {
			List<Service> imageServices = image.getServices();
			final String hostUuid = imageServices.get(0).getHostUuid();
			routingkey = getRoutingKey(hostUuid);

			for (Service service:imageServices) {
				logger.info("sourePath:"+sourcePath+"do copy to service:"+service.toString()+" destpath: "+destPath);
				vp.authorizeImg(routingkey, sourcePath, service.getHostIp(), destPath, rpcExtra);
			}

			return image;
		} catch (RpcException e) {
			if (e instanceof RpcTimeoutException) {
				String why = "volume-scheduler no response from volume-provider";
				logger.error(why);
				loller.error(LolLogUtil.AUTHORIZE_IMAGE, why, rpcExtra);
				throw new RpcTimeoutException(why);
			}
			String why ="volume-scheduler authorize image failed" + image.getUuid();
			logger.warn(why,e);
//			vp.deleteVolume(routingkey, image.getUuid(), rpcExtra);
			loller.error(LolLogUtil.AUTHORIZE_IMAGE, why+e.getMessage(), rpcExtra);
			throw new RpcException(why,e);
		} catch (Exception e){
			String why ="volume-scheduler imageProxy failed, image: " + image;
			logger.warn(why,e);
//			vp.deleteVolume(routingkey, image.getUuid(), rpcExtra);
			loller.error(LolLogUtil.AUTHORIZE_IMAGE, why+e.getMessage(), rpcExtra);
			throw new RpcException(why,e);
		}


	}


	/**
	 * {@inheritDoc}
	 */
	public void deleteImageVolume(String volumeUUID, VmImage image, final RpcExtra rpcExtra) throws RpcException {
		logger.info("start to delete image volume...");
		String routingkey = "";
		try {
			final VmVolume volume = db.getVolume(volumeUUID);
			routingkey = getRoutingKey(volume.getHostUuid());
			
			if(volume.getStatus() != VmVolumeStatusEnum.AVAILABLE){
				String why ="invalid volume status";
				logger.error(why);
				loller.error(LolLogUtil.DELETE_IMG, why+"volume:"+volume, rpcExtra);
				throw new RpcException(why);
			}
			//delete the rebased file 
			vp.deleteVolume(routingkey, image.getUuid(), rpcExtra);
			logger.info("delete image volume finished!!!");
			
		} catch (RpcException e) {
			if (e instanceof RpcTimeoutException) {
				String why = "volume-scheduler no response from volume-provider";
				logger.error(why);
				loller.error(LolLogUtil.DELETE_IMG, why, rpcExtra);
				throw new RpcTimeoutException(why);
			}
			String why ="volume-scheduler delete volume failed" + image.getUuid();
			logger.warn(why,e);
			vp.deleteVolume(routingkey, image.getUuid(), rpcExtra);
			loller.error(LolLogUtil.DELETE_IMG, why+e.getMessage(), rpcExtra);
			throw new RpcException(why,e);
		}catch (Exception e){
			String why ="volume-scheduler imageProxy failed, image: " + image;
			logger.warn(why,e);
			vp.deleteVolume(routingkey, image.getUuid(), rpcExtra);
			loller.error(LolLogUtil.DELETE_IMG, why+e.getMessage(), rpcExtra);
			throw new RpcException(why,e);
		}
	}
	
	/**
	 * 更新新镜像
	 * @param newImages	要更新的镜像
	 * 		newImages中要更新的字段有：size, status
	 * @return	新保存的镜像记录，填充了部分字段：
	 */
	public void updateImage(List<VmImage> newImages, String volumeUUID, VmImageStatusEnum status, RpcExtra rpcExtra) throws IllegalRpcArgumentException, RpcException{
		logger.info("update image size and status : "+ newImages);
		try {
			final VmVolume volume = db.getVolume(volumeUUID);
			if(volume.getStatus() != VmVolumeStatusEnum.AVAILABLE){
				String why ="invalid volume status";
				logger.error(why);
				loller.error(LolLogUtil.DELETE_IMG, why+"volume:"+volume, rpcExtra);
				throw new RpcException(why);
			}
			for(VmImage image : newImages){
				try {
					image.setSize(volume.getSize());
				    image.setStatus(status);
				    db.update(image);
				    logger.info("update image success, image status : " + image.getStatus());
			    } catch (Exception e) {
					logger.error("update image " + image.getName() + " failed", e);
					try {
						loller.error(LolLogUtil.DELETE_IMG, "update image " + image.getName() + " failed", rpcExtra);
					} catch (RpcException e1) {
						logger.error("write lol log failed");
					}
			    }
			}
		} catch (Exception e) {
			String why = "update image failed!!!";
			logger.warn(why,e);
			loller.error(LolLogUtil.UPDATE_IMAGE, "update new image failed!", rpcExtra);
			throw new RpcException(why, e);
		}
	}
	
	/*public VmImage publishImage(String volumeUUID, VmImage image, final RpcExtra rpcExtra)
			throws RpcException {*/
	public VmImage publishImage(String volumeUUID, VmImage image, Integer clusterId, final RpcExtra rpcExtra)
			throws RpcException {
		logger.info("Start to publishImage: " +volumeUUID + "; image: "+image);
		String routingkey = "";
		try {
			//1 check status
			final VmVolume volume = db.getVolume(volumeUUID);
			routingkey = getRoutingKey(volume.getHostUuid());
			
			if(volume.getStatus() != VmVolumeStatusEnum.AVAILABLE){
				String why ="invalid volume status";
				logger.error(why);
				loller.error(LolLogUtil.PUBLISH_IMAGE, why+"volume:"+volume, rpcExtra);
				throw new RpcException(why);
			}
			
			final VmImage tmpImage = image;
			final Integer selectClusterId = clusterId;
			//将新发布的镜像拷贝到每个有volume-provider的物理主机
			new Thread(
				new Runnable() {
					public void run() {
						String imgService = tmpImage.getServices().get(0).getHostIp();
						/*List<Service> vpServices = db.getRunningServiceList(volume.getAvailabilityZoneId(), -1, Service.ServiceTypeEnum.VOLUME_PROVIDER);*/
						List<Service> vpServices = db.getRunningServiceList(volume.getAvailabilityZoneId(), selectClusterId, Service.ServiceTypeEnum.VOLUME_PROVIDER);
						for (Service vpservice : vpServices) {
							try {
								logger.info("when publish image, copy img to volume-provider's ip " + vpservice.getHostIp());
								vp.downloadImg(getRoutingKey(vpservice.getHostUuid()), imgService, tmpImage.getDirectory(), false, rpcExtra);
								logger.info("copy image " + tmpImage.getName() + " to volume-provider " + vpservice.getHostIp() + " succeed");
							} catch (RpcException e) {
								logger.error("copy img to volume-provider " + vpservice.getHostIp() + " failed", e);
								try {
									loller.error(LolLogUtil.PUBLISH_IMAGE, "copy img to volume-provider " + vpservice.getHostIp() + " failed", rpcExtra);
								} catch (RpcException e1) {
									logger.error("write lol log failed");
								}
							}
						}
						logger.info("copy img to volume-provider finished!");
					}
				}).start();
			
			logger.info("Successfully publishImage: " +volumeUUID + " image: "+image);
			loller.info(LolLogUtil.PUBLISH_IMAGE, "Successfully publishImage: " +volumeUUID + " image: "+image, rpcExtra);
			return image;
		} catch (RpcException e) {
			if (e instanceof RpcTimeoutException) {
				String why = "volume-scheduler no response from volume-provider";
				logger.error(why);
				loller.error(LolLogUtil.PUBLISH_IMAGE, why, rpcExtra);
				throw new RpcTimeoutException(why);
			}
			String why ="volume-scheduler publishImage failed";
			logger.warn(why,e);
//			image.setStatus(VmImageStatusEnum.ERROR);
//			imageProxy.update(image);
			vp.deleteVolume(routingkey, image.getUuid(), rpcExtra);
			loller.error(LolLogUtil.PUBLISH_IMAGE, why+e.getMessage(), rpcExtra);
			throw new RpcException(why,e);
		} catch (Exception e) {
			String why ="volume-scheduler imageProxy failed, image: " + image;
			logger.warn(why,e);
			vp.deleteVolume(routingkey, image.getUuid(), rpcExtra);
			loller.error(LolLogUtil.PUBLISH_IMAGE, why+e.getMessage(), rpcExtra);
			throw new RpcException(why,e);
		}
	}
	
	public VmVolume revertVolume(String volumeUUID, RpcExtra rpcExtra)
			throws RpcException {
		logger.info("Starting revertVolume: " +volumeUUID);
		try {
			//1.check
			VmVolume volume = db.getVolume(volumeUUID);
			logger.info("reverting: " + volume);
			//2.before
			volume.setStatus(VmVolumeStatusEnum.CREATING);
			db.update(volume);
			
			//3.do job
			List<Host> hosts = selectHost(volume);
			boolean success = false;
			VmImage image = volume.getImage();
			// do create
			for (Host host : hosts) {
				try{
					if(volume.getVolumeType().equals(VmVolumeTypeEnum.ISO)){
						logger.error("failed to revert on uuid" + volumeUUID+" it's a iso file");
						loller.error(LolLogUtil.REVERT_VOLUME, "failed to revert on uuid" + volumeUUID+" it's a iso file", rpcExtra);
						return null;
//						String ishost = getImageServiceHost(image.getUuid(), rpcExtra);
//						VmVolume v = vp.downloadImg(getRoutingKey(host.getUuid()), ishost, image.getDirectory(), false, rpcExtra);
//						volume.setProviderLocation(v.getProviderLocation());
					} else{
						revertVolume(host, volume, image, rpcExtra);
					}
					success = true;
					break;
					
				} catch (RpcException e) {
					loller.error(LolLogUtil.REVERT_VOLUME, "failed to revert on uuid" + volumeUUID, rpcExtra);
					logger.error("failed to revert on uuid" + volumeUUID);
				}
			}
			
			//4. update status
			if(success){
				volume.setStatus(VmVolumeStatusEnum.AVAILABLE);
				db.update(volume);
				logger.info("Successfully revertVolume: " +volume);
				loller.info(LolLogUtil.REVERT_VOLUME, "Successfully revertVolume: " +volume, rpcExtra);
				return db.getVolume(volumeUUID);
			} else{
				logger.warn("Failed to revert volume :"+ volumeUUID);
				volume.setStatus(VmVolumeStatusEnum.ERROR);
				db.update(volume);
				loller.error(LolLogUtil.REVERT_VOLUME, "Failed to revert volume :"+ volumeUUID, rpcExtra);
				throw new RpcException("failed to revert VmVolume");
			}
		} catch (RpcException e) {
			if (e instanceof RpcTimeoutException) {
				String why = "volume-scheduler no response from volume-provider";
				logger.error(why);
				loller.error(LolLogUtil.REVERT_VOLUME, why, rpcExtra);
				throw new RpcTimeoutException(why);
			}
			String why = "volume-scheduler failed to revert Volume";
			logger.error(why,e);
			loller.error(LolLogUtil.REVERT_VOLUME, why + e.getMessage(), rpcExtra);
			throw new RpcException(why, e);
		} catch (Exception e) {
			String why = "volume-scheduler failed to revert Volume in db ";
			logger.error(why, e);
			loller.error(LolLogUtil.REVERT_VOLUME, why + e.getMessage(), rpcExtra);
			throw new RpcException(why,e);
		}
	}

	public VmVolume modVmHostName(String volumeUUID, String newHostName, VmImageOSTypeEnum OSType, RpcExtra rpcExtra) throws RpcException {
		logger.info("Starting modify vm hostname: volumeUUID:" +volumeUUID +",newHostName= "+newHostName);
		try {
			//1.check
			VmVolume volume = db.getVolume(volumeUUID);
			if(!volume.getStatus().equals(VmVolumeStatusEnum.AVAILABLE)){
				loller.error(LolLogUtil.MOD_VM_HOSTNAME, "cannot modify an unAvailable volume:"+volumeUUID+"'s hostname!", rpcExtra);
				throw new RpcException("volume-scheduler cannot modify an unAvailable volume's hostname");
			}
			
			//2.do job
			String routingkey = getRoutingKey(volume.getHostUuid());
			logger.info(routingkey);
			volume = vp.modVmHostName(routingkey, volumeUUID, newHostName, OSType, rpcExtra);
			logger.info("Successfully modify vm hostname :"+ volumeUUID);
			loller.info(LolLogUtil.MOD_VM_HOSTNAME, "Successfully modify vm hostname  :"+ volumeUUID, rpcExtra);
			return volume;
		} catch (RpcException e) {
			if (e instanceof RpcTimeoutException) {
				logger.error("volume-scheduler no response from volume-provider");
				loller.error(LolLogUtil.MOD_VM_HOSTNAME, "volume-scheduler no response from volume-provider", rpcExtra);
				throw new RpcTimeoutException("volume-scheduler no response from volume-provider");
			}
			String why = "volume-scheduler failed to modify "; 
			logger.warn(why, e);
			loller.error(LolLogUtil.MOD_VM_HOSTNAME, why+e.getMessage(), rpcExtra);
			throw new RpcException(why, e);
		} catch (Exception e) {
			String why = "volume-scheduler failed to get info from db";
			logger.warn(why, e);
			loller.error(LolLogUtil.MOD_VM_HOSTNAME, why+e.getMessage(), rpcExtra);
			throw new RpcException(why, e);
		}
		
	}
	
	public VmVolume modVmPasswd(String volumeUUID, String name, String newPasswd, VmImageOSTypeEnum OSType, RpcExtra rpcExtra) throws RpcException {
		logger.info("Starting modify vm password: volumeUUID:" +volumeUUID +",name= "+name);
		try {
			//1.check
			VmVolume volume = db.getVolume(volumeUUID);
			if(!volume.getStatus().equals(VmVolumeStatusEnum.AVAILABLE)){
				loller.error(LolLogUtil.MOD_VM_PASSWD, "cannot modify an unAvailable volume:"+volumeUUID+"'s password!", rpcExtra);
				throw new RpcException("volume-scheduler cannot modify an unAvailable volume's password");
			}
			
			//2.do job
			String routingkey = getRoutingKey(volume.getHostUuid());
			logger.info(routingkey);
			volume = vp.modVmPasswd(routingkey, volumeUUID, name, newPasswd, OSType, rpcExtra);
			logger.info("Successfully modify vm password :"+ volumeUUID);
			loller.info(LolLogUtil.MOD_VM_PASSWD, "Successfully modify vm password  :"+ volumeUUID, rpcExtra);
			return volume;
		} catch (RpcException e) {
			if (e instanceof RpcTimeoutException) {
				logger.error("volume-scheduler no response from volume-provider");
				loller.error(LolLogUtil.MOD_VM_PASSWD, "volume-scheduler no response from volume-provider", rpcExtra);
				throw new RpcTimeoutException("volume-scheduler no response from volume-provider");
			}
			String why = "volume-scheduler failed to modify "; 
			logger.warn(why, e);
			loller.error(LolLogUtil.MOD_VM_PASSWD, why+e.getMessage(), rpcExtra);
			throw new RpcException(why, e);
		} catch (Exception e) {
			String why = "volume-scheduler failed to get info from db";
			logger.warn(why, e);
			loller.error(LolLogUtil.MOD_VM_PASSWD, why+e.getMessage(), rpcExtra);
			throw new RpcException(why, e);
		}
		
	}
	
	/**
	  *获取某个群组中可以发布镜像的集群id，判断条件是所要发布的镜像是否已经存在于该集群上
	  *@param imageGroupIdList 镜像所在的所有群组Id
	  *@param groupIdList  镜像所要发布的群组id
	  *@param  rpcExtra
	  *@return clusterIds  镜像还未存在的集群Id
	  *@author hgm
	  */
	 public List<Integer> gainClusterIdList( List<Integer> imageGroupIdList, List<Integer> groupIdList, RpcExtra rpcExtra) throws RpcException{
		 logger.info("start to gain cluster ids...");
		 try {
			Set<Integer> existClusters = new HashSet<Integer>();
			Set<Integer> needClusters = new HashSet<Integer>();
			List<Integer> finalClusters = new ArrayList<Integer>();
			for(Integer groupId  : imageGroupIdList) {
				String[] clusterIds = db.getVmGroupById(groupId).getAvailableClusters().split(",");
				for(String clusterId : clusterIds)
					existClusters.add(Integer.valueOf(clusterId));
			}
			for(Integer groupId  : groupIdList) {
				String[] clusterIds = db.getVmGroupById(groupId).getAvailableClusters().split(",");
				for(String clusterId : clusterIds)
					needClusters.add(Integer.valueOf(clusterId));
			}
			for(Integer i : needClusters) {
				if(!existClusters.contains(i))
					finalClusters.add(i);
			}
			logger.info("gain cluster ids success, total cluster : " + finalClusters.size());
			return finalClusters;
		}catch (Exception e) {
			String why = "volume-scheduler failed to get info from db";
			logger.warn(why, e);
			loller.error(LolLogUtil.GET_GROUPCLUSTERIDS, why+e.getMessage(), rpcExtra);
			throw new RpcException(why, e);
		}
	 }
	 
	/**
	 *根据群组Id获取这些群组对应的集群集合元素的字符串，每个集群Id用||包围，如集群2：|2|
	 *@param groupIdList 群组Id的list
	 *@return strClusterIds  群组集合中元素连接起来的字符串
	 *@author hgm
	 */
	public String strClusterIds(List<Integer> groupIdList, RpcExtra rpcExtra) throws RpcException {
		if(groupIdList==null || groupIdList.size()==0) {
			return "";
		}
		
		String strClusterIds = "";
		try {
			for(Integer groupId : groupIdList) {
				VmGroup vmGroup = db.getVmGroupById(groupId);
				String[] clusters = vmGroup.getAvailableClusters().split(",");
				for(Integer index = 0; index < clusters.length; index++) {
					if(!strClusterIds.contains("|"+clusters[index]+"|")) {
						strClusterIds += "|"+clusters[index]+"|";
					}
				}
			}
			logger.info(strClusterIds);
			return strClusterIds;
		} catch (Exception e) {
			String why = "volume-scheduler failed to get info from db";
			logger.warn(why, e);
			loller.error(LolLogUtil.GET_VMGROUP, why+e.getMessage(), rpcExtra);
			throw new RpcException(why, e);
		}
	}
	
	/**
	 *返回所要发布的模板镜像所在的群组ID，判断依据就是所要发布的镜像模板的md5值
	 *@param image  所要上传的镜像
	 *@param  rpcExtra
	 *@return md5sum   镜像的md5sum值
	 *@author hgm
	 */
	 public String  gainImageMd5sum(String volumeUUID, VmImage image, RpcExtra rpcExtra) throws RpcException {
		//获取镜像模板的md5sum值
		 String routingkey = "";
		 try {
			 	final VmVolume volume = db.getVolume(volumeUUID);
				routingkey = getRoutingKey(volume.getHostUuid());
				logger.info("convert image routingkey : " + routingkey);
				if(volume.getStatus() != VmVolumeStatusEnum.AVAILABLE){
					String why ="invalid volume status";
					logger.error(why);
					loller.error(LolLogUtil.CONVERT_IMG_FORMAT, why+"volume:"+volume, rpcExtra);
					throw new RpcException(why);
				}
			 //rebase
			 logger.info("cloned old volume");
			 //获取所要发布的镜像的md5值，即整合后的镜像的md5值，
			 //为了减少访问volume-provider的次数，将整合和计算都放在vp.gainImgMd5sum中进行
			 logger.info("image directory : " + image.getDirectory());
			 String md5sum = vp.gainImgMd5sum(routingkey, volume, image, rpcExtra);
			 
			 logger.info("volume scheduler-->image md5sum :" + md5sum);
			 return md5sum;
		 } catch (RpcException e) {
			if (e instanceof RpcTimeoutException) {
				String why = "volume-scheduler no response from volume-provider";
				logger.error(why);
				loller.error(LolLogUtil.CONVERT_IMG_FORMAT, why, rpcExtra);
				throw new RpcTimeoutException(why);
			}
			String why ="volume-scheduler convert image format failed : " + image.getUuid();
			logger.warn(why,e);
			vp.deleteVolume(routingkey, image.getUuid(), rpcExtra);
			loller.error(LolLogUtil.CONVERT_IMG_FORMAT, why+e.getMessage(), rpcExtra);
			throw new RpcException(why,e);
		} catch (Exception e) {
			String why = "volume-scheduler failed to get image md5sum";
			logger.warn(why, e);
			loller.error(LolLogUtil.GET_IMG_MD5SUM, why+e.getMessage(), rpcExtra);
			throw new RpcException(why, e);
		}
	 }
	
	/**
	 *返回所要发布的模板镜像所在的群组ID，判断依据就是所要发布的镜像模板的md5值
	 *@param imageMd5sum  镜像的md5sum值
	 *@param  rpcExtra
	 *@return groupIds   镜像所在的所有的群组Id
	 *@author hgm
	 */
	 public List<Integer> gainImageGroupIds(String imageMd5sum, RpcExtra rpcExtra) throws RpcException {
		 try {
			 logger.info("gain image group ids by image md5sum : " + imageMd5sum);
			 List<? extends VmImage> vmImageList = db.getImageByMd5sum(imageMd5sum.trim());
			 List<Integer> groupIds = new ArrayList<Integer>();
			 if(vmImageList != null && vmImageList.size()>0){
				 for(VmImage vmImage : vmImageList) {
					 if(vmImage.getStatus().equals(VmImageStatusEnum.CREATING) || 
						vmImage.getStatus().equals(VmImageStatusEnum.AVAILABLE))
					 groupIds.add(vmImage.getGroupId());
				 }
			 }
			 logger.info("groupIds size :" + groupIds.size());
			 return groupIds;
		 } catch (Exception e) {
			String why = "volume-scheduler failed to get info from db";
			logger.warn(why, e);
			loller.error(LolLogUtil.GET_IMG_GROUP, why+e.getMessage(), rpcExtra);
			throw new RpcException(why, e);
		}
	 }
	 /**
	 * Description 删除镜像文件
	 * @param path 相对路径
	 * @return
	 * @throws IOException
	 * @author GongLingpu
	 */
	public boolean deleteImageOnServer(String imageUuid,String path) throws IOException{
		 logger.info("in volumescheduler delete image path:"+path);
		 VmVolumeProxy vmvolume = (VmVolumeProxy)ConnectionFactory.getDBProxy(VmVolumeProxy.class);
		 String routingkey = "";
		try {
			String hostUuid = vmvolume.getHostUuidByImageUuid(imageUuid);
			routingkey = getRoutingKey("782BCB435306");
			logger.info("convert image routingkey : " + routingkey);
		} catch (Exception e) {
			logger.error("get routingkey error");
			e.printStackTrace();
		}
			
		 return vp.deleteImage(routingkey,path);
		 
	 }
	public static void main(String[] args) {
		String routingkey = "";
		routingkey = new VolumeScheduler().getRoutingKey("782BCB435306");
		System.out.println(routingkey);
	}

	public String KeepAlive() throws Exception {
		

		logger.info(String.format("---------------------keepalive-------------------"));
		logger.info(String.format("---------------------"+this.getClass()+":success-------------------"));
				
		//HostProxy hostproxy = (HostProxy)ConnectionFactory.getDBProxy(HostProxy.class);
		HostProxy hostproxy = ConnectionFactory.getHostProxy();
		
		boolean flag = false;
		List<Host> HostList= (List<Host>)hostproxy.findAll(false, false, false);
		List<String> RoutingKeyList = new ArrayList<String>();
		for(int i=0;i<HostList.size();i++)
		{
			Host tmp =(Host)HostList.get(i);
			if(tmp.getType()==Host.HostTypeEnum.COMPUTE_NODE)
				RoutingKeyList.add(tmp.getUuid());
		}
		if (RoutingKeyList.size()>0) 
			flag= true;
		for(int i=0;i<RoutingKeyList.size();i++)
		{
			String routingkey = RoutingKeyGenerator.getRoutingKey(RoutingKeys.VOLUME_PROVIDER_PRE, RoutingKeyList.get(i));
			try{
				vp.KeepAlive(routingkey);
			}catch (Exception e)
			{
				logger.info(String.format("---------------------keepalive-------------------"));
				logger.info(String.format("---------------------"+routingkey+":fail-------------------"));
				flag=false;
			}				
		}		
		if(flag)
			return "success";
		return "fail";
		
		
	}
}
