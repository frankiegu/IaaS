package appcloud.common.service;

import java.io.IOException;

import appcloud.common.model.RpcExtra;
import appcloud.common.model.VmImage;
import appcloud.common.model.VmImage.VmImageOSTypeEnum;
import appcloud.common.model.VmImageBack;
import appcloud.common.model.VmVolume;
import appcloud.rpc.ampq.annotation.RPCTimeout;
import appcloud.rpc.ampq.annotation.RoutingKeyAnnotation;
import appcloud.rpc.tools.RpcException;

/**
 * @author huahui
 * 
 */
public interface VolumeProviderService {

	/**
	 * 创建一个盘，如果首次用这个母镜像，这个操作比较耗时，往后在重复用这个母镜像创建盘就很快了
	 * 网络状况好的话，机器之间的拷贝操作相对就快。（下同）
	 * 
	 * @param routingKey
	 * @param uuid
	 *            example:abcd1234.img
	 * @param imageServerHost
	 *            example:192.168.11.132
	 * @param baseImage
	 *            example:images/c/e/rhel.img 
	 *            special condition: baseImage is
	 *            null for create a disk space request,here blockSize is a must
	 * @param blockSize
	 *            example:5.6 or null
	 * @return VmVolume:providerLocation,volumeType
	 * @throws RpcException
	 */
	@RoutingKeyAnnotation(index = 0)
	@RPCTimeout(timeout = 1000)
	public VmVolume createVolume(String routingKey, String uuid,
			String imageServerHost, String baseImage, Float blockSize, RpcExtra rpcExtra)
			throws RpcException;

	@RoutingKeyAnnotation(index = 0)
	@RPCTimeout(timeout = 1000)
	public VmVolume createWindowsVolume(String routingKey, String uuid, String imageServerHost, String baseImage, Float blockSize, RpcExtra rpcExtra)
			throws RpcException;

	/**
	 *
	 * 为系统盘创建增量镜像，当前系统盘作为backfile，新创建的镜像作为虚拟机硬盘当前使用的硬盘
	 *
	 * @param routingKey
	 * @param uuid
	 * @param rpcExtra
	 * @return
	 * @throws RpcException
	 */
	@RoutingKeyAnnotation(index = 0)
	@RPCTimeout(timeout = 1000)
	public VmImageBack createVolumeImageBack(String routingKey,String backUuid,String activeUuid,Float size,RpcExtra rpcExtra) throws RpcException;

	/**
	 * 格式化硬盘或者格式化系统
	 * @param routingKey
	 * @param uuid
	 * @param imageServerHost
	 * @param baseImage
	 * @param blockSize
	 * @param rpcExtra
	 * @return
	 * @throws RpcException
	 */
	@RoutingKeyAnnotation(index = 0)
	@RPCTimeout(timeout = 90)
	public VmVolume revertVolume(String routingKey, String uuid,
			String imageServerHost, String baseImage, Float blockSize, RpcExtra rpcExtra)
			throws RpcException;

	/***
	 * 从磁盘删除盘
	 * @param routingKey
	 * @param uuid example:abcd1234.img
	 * @return
	 * @throws RpcException
	 */
	@RoutingKeyAnnotation(index = 0)
	@RPCTimeout(timeout = 10)
	public VmVolume deleteVolume(String routingKey, String uuid, RpcExtra rpcExtra)
			throws RpcException;

	/***
	 * 盘上做一个快照，快照直接保存在盘上
	 * @param routingKey
	 * @param uuid example:abcd1234.img
	 * @param snapshotTag 字符串数字都可以，不要是一些奇怪的字符
	 * @return
	 * @throws RpcException
	 */
	@RoutingKeyAnnotation(index = 0)
	public VmVolume createSnapshot(String routingKey, String uuid,
			String snapshotTag, RpcExtra rpcExtra) throws RpcException;

	/***
	 * 回滚到之前做的快照
	 * @param routingKey
	 * @param uuid example:abcd1234.img
	 * @param snapshotTag
	 * @return
	 * @throws RpcException
	 */
	@RoutingKeyAnnotation(index = 0)
	public VmVolume applySnapshot(String routingKey, String uuid,
			String snapshotTag, RpcExtra rpcExtra) throws RpcException;

	/***
	 * 删除之前做的快照
	 * @param routingKey
	 * @param uuid example:abcd1234.img
	 * @param snapshotTag
	 * @return
	 * @throws RpcException
	 */
	@RoutingKeyAnnotation(index = 0)
	public VmVolume deleteSnapshot(String routingKey, String uuid,
			String snapshotTag, RpcExtra rpcExtra) throws RpcException;

	/**
	 * 只能增加空间，镜像格式必须是raw
	 * 
	 * @param routingKey
	 * @param uuid
	 * @param blockSize
	 *            example:3.1(should bigger than zero)
	 * @return VmVolume:providerLocation
	 * @throws RpcException
	 */
	@RoutingKeyAnnotation(index = 0)
	@RPCTimeout(timeout = 20)
	public VmVolume resizeRawImg(String routingKey, String uuid, Float blockSize, RpcExtra rpcExtra)
			throws RpcException;

	/**
	 * 改变镜像格式，这个操作相对比较耗时
	 * 
	 * @param routingKey
	 * @param targetFormat
	 *            example:qcow2 or raw
	 * @param srcPath
	 * @param destPath
	 * @return VmVolume:providerLocation,volumeType
	 * @throws RpcException
	 */
	@RoutingKeyAnnotation(index = 0)
	@RPCTimeout(timeout = 360)
	public VmVolume convertImgFormat(String routingKey, String targetFormat,
			String uuid, String destUuid, RpcExtra rpcExtra) throws RpcException;

	/**
	 * 发布用户镜像到imgserver，拷贝操作，比较耗时
	 * 
	 * @param routingKey
	 * @param uuid
	 * @param host
	 * @param destPath  example:images/s/e/user.img
	 * @return VmVolume:providerLocation
	 * @throws RpcException
	 */
	@RoutingKeyAnnotation(index = 0)
	@RPCTimeout(timeout = 1100)
	public VmVolume releaseImg(String routingKey, String uuid, String host,
			String destPath, RpcExtra rpcExtra) throws RpcException;

	/**
	 * 授权镜像，执行copy操作
	 * @param sourcefilePath
	 * @param hostIp
	 * @param destPath
	 * @param rpcExtra
	 * @return
	 * @throws RpcException
	 */
	@RoutingKeyAnnotation(index = 0)
	@RPCTimeout(timeout = 1100)
	public VmVolume authorizeImg(String routingKey, String sourcefilePath, String hostIp,
							String destPath, RpcExtra rpcExtra) throws RpcException;

	/***
	 * 
	 * @param routingKey
	 * @param uuid
	 * @param host 目标机器host，null则为本机拷贝
	 * @param destUuid 
	 * @param baseImage example: images/c/e/rhel.img 
	 * @return VmVolume:providerLocation
	 * @throws RpcException
	 */
	@RoutingKeyAnnotation(index = 0)
	@RPCTimeout(timeout = 360)
	public VmVolume copyImg(String routingKey, String uuid, String host, String destUuid, String baseImage, RpcExtra rpcExtra)
			throws RpcException;
	
	/***
	 * 从imageserver上把iso,img拷贝下来
	 * 
	 * @param routingKey
	 * @param host   imageserver's host
	 * @param srcPath example: iso/a/c/ubuntu.iso
	 * @param overwrite true for overwirte, null or false no overwirte 
	 * @return VmVolume:providerLocation
	 * @throws RpcException
	 */
	@RoutingKeyAnnotation(index = 0)
	@RPCTimeout(timeout = 900)
	public VmVolume downloadImg(String routingKey, String host, String srcPath, Boolean overwrite, RpcExtra rpcExtra)
			throws RpcException;

	/**
	 * 修改虚拟机的hostname
	 * @param routingKey
	 * @param uuid volume's uuid
	 * @param newHostName  new hostName
	 * @param rpcExtra
	 * @return
	 * @throws RpcException
	 */
	@RPCTimeout(timeout = 90)
	@RoutingKeyAnnotation(index = 0)
	public VmVolume modVmHostName(String routingKey, String uuid, String newHostName, VmImageOSTypeEnum OSType, RpcExtra rpcExtra) throws RpcException;
	
	/**
	 * 修改虚拟机的密码
	 * @param routingKey
	 * @param uuid volume's uuid
	 * @param name  admin's name
	 * @param newPasswd  new password
	 * @param rpcExtra
	 * @return
	 * @throws RpcException
	 */
	@RPCTimeout(timeout = 90)
	@RoutingKeyAnnotation(index = 0)
	public VmVolume modVmPasswd(String routingKey, String uuid, String name, String newPasswd, VmImageOSTypeEnum OSType, RpcExtra rpcExtra) throws RpcException;
	
	/**
	 * 获取镜像模板的Md5sum值
	 * @param routingKey 
	 * @return strMd5sum 镜像模板的Md5sum值
	 * @throws RpcException
	 * @author hgm
	 */
	@RPCTimeout(timeout = 1200)
	public String getImgMd5sum(String routingKey, String imageUuid, RpcExtra rpcExtra) throws RpcException;
	
	@RoutingKeyAnnotation(index = 0)
	@RPCTimeout(timeout = 3000)
	public String gainImgMd5sum(String routingKey, VmVolume volume, VmImage image, RpcExtra rpcExtra) throws RpcException;

	@RoutingKeyAnnotation(index = 0)
	@RPCTimeout(timeout = 3000)
	public boolean deleteImage(String routingKey,String path) throws IOException;
	
	@RoutingKeyAnnotation(index = 0)
	@RPCTimeout(timeout = 5)
	public String KeepAlive(String routingKey) throws Exception ;
}
