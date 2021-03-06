package appcloud.openapi.check;

import java.util.Map;

/**
 *	此类用于用户实际操作云平台资源前进行的权限检查
 *	@author hgm
 */
public interface InstanceOperateCheck {
	/**
	 *	创建云主机前，检查用户操作权限
	 *	@param paramsMap 接口中所有参数的map
	 *	@return 返回检查结果 Map<String, String>
	 *	@author hgm
	 */
	public Map<String, String> checkCreateInstance(Map<String, String> paramsMap) throws Exception;
	/**
	 *	恢复云主机，检查用户操作权限
	 *	@param paramsMap 接口中所有参数的map
	 *	@return 返回检查结果 Map<String, String>
	 *	@author hgm
	 */
	public Map<String, String> checkRecoveryInstance(Map<String, String> paramsMap) throws Exception;
	/**
	 *	启动云主机前，检查用户操作权限
	 *	@param paramsMap 接口中所有参数的map
	 *	@return 返回检查结果 Map<String, String>
	 *	@author hgm
	 */
	public Map<String, String> checkStartInstance(Map<String, String> paramsMap) throws Exception;
	/**
	 *	停止云主机前，检查用户操作权限
	 *	@param paramsMap 接口中所有参数的map
	 *	@return 返回检查结果 Map<String, String>
	 *	@author hgm
	 */
	public Map<String, String> checkStopInstance(Map<String, String> paramsMap) throws Exception;
	/**
	 *	重启云主机前，检查用户操作权限
	 *	@param paramsMap 接口中所有参数的map
	 *	@return 返回检查结果 Map<String, String>
	 *	@author hgm
	 */
	public Map<String, String> checkRebootInstance(Map<String, String> paramsMap) throws Exception ;
	/**
	 *	删除云主机前，检查用户操作权限
	 *	@param paramsMap 接口中所有参数的map
	 *	@return 返回检查结果 Map<String, String>
	 *	@author hgm
	 */
	public Map<String, String> checkDeleteInstance(Map<String, String> paramsMap) throws Exception ;
	/**
	 *	重置云主机系统前，检查用户操作权限
	 *	@param paramsMap 接口中所有参数的map
	 *	@return 返回检查结果 Map<String, String>
	 *	@author luofan
	 */
	public Map<String, String> checkResetInstance(Map<String, String> paramsMap) throws Exception ;
	/**
	 *	iso弹出前，检查用户操作权限
	 *	@param paramsMap 接口中所有参数的map
	 *	@return 返回检查结果 Map<String, String>
	 *	@author luofan
	 */
	public Map<String, String> checkIsoDetach(Map<String, String> paramsMap) throws Exception ;
	/**
	 *	iso安装前，检查用户操作权限
	 *	@param paramsMap 接口中所有参数的map
	 *	@return 返回检查结果 Map<String, String>
	 *	@author luofan
	 */
	public Map<String, String> checkIsoBoot(Map<String, String> paramsMap) throws Exception ;
	/**
	 *  修改云主机属性前，检查用户操作权限
	 *	@param paramsMap 接口中所有参数的map
	 *	@return 返回检查结果 Map<String, String>
	 *	@author luofan
	 */
	public Map<String, String> checkModifyInstanceAttribute(Map<String, String> paramsMap) throws Exception;
	/**
	 *  修改云主机资源前，检查用户操作权限
	 *	@param paramsMap 接口中所有参数的map
	 *	@return 返回检查结果 Map<String, String>
	 *	@author luofan
	 */
	public Map<String, String> checkModifyInstanceResource(Map<String, String> paramsMap) throws Exception;
	/**
	 *  修改云主机防火墙规则前，检查用户操作权限
	 *	@param paramsMap 接口中所有参数的map
	 *	@return 返回检查结果 Map<String, String>
	 *	@author luofan
	 */
	public Map<String, String> checkModifyInstanceSecurityGroup(Map<String, String> paramsMap) throws Exception;
	/**
	 *  修改云主机付费方式前，检查用户操作权限
	 *	@param paramsMap 接口中所有参数的map
	 *	@return 返回检查结果 Map<String, String>
	 *	@author luofan
	 */
	public Map<String, String> checkModifyInstanceChargeType(Map<String, String> paramsMap) throws Exception;
	/**
	 *  查看云服务器实例的监控信息前，检查用户操作权限
	 *	@param paramsMap 接口中所有参数的map
	 *	@return 返回检查结果 Map<String, String>
	 */
	public Map<String, String> checkMonitor(Map<String, String> paramsMap) throws Exception;
	/**
	 *  迁移云主机实例前，检查用户操作权限
	 *	@param paramsMap 接口中所有参数的map
	 *	@return 返回检查结果 Map<String, String>
	 *	@author luofan
	 */
	public Map<String, String> checkMigrate(Map<String, String> paramsMap) throws Exception;
	/**
	 *  在线迁移云主机实例前，检查用户操作权限
	 *	@param paramsMap 接口中所有参数的map
	 *	@return 返回检查结果 Map<String, String>
	 *	@author luofan
	 */
	public Map<String, String> checkOnlineMigrate(Map<String, String> paramsMap) throws Exception;
	/**
	 *  挂起云主机实例前，检查用户操作权限
	 *	@param paramsMap 接口中所有参数的map
	 *	@return 返回检查结果 Map<String, String>
	 *	@author luofan
	 */
	public Map<String, String> checkSuspend(Map<String, String> paramsMap) throws Exception;
	/**
	 *  恢复迁移云主机实例前，检查用户操作权限
	 *	@param paramsMap 接口中所有参数的map
	 *	@return 返回检查结果 Map<String, String>
	 *	@author luofan
	 */
	public Map<String, String> checkResume(Map<String, String> paramsMap) throws Exception;
	
	/**
	 * 查询云主机，检查用户操作权限
	 * @param paramsMap
	 * @return
	 * @throws Exception
	 */
	public Map<String, String> checkDescInstancesParams(Map<String, String> paramsMap) throws Exception;
	
	/**
	 * 续费云主机，检查用户操作权限
	 * @param paramsMap
	 * @return
	 * @throws Exception
	 */
	public Map<String, String> checkRenewInstance(Map<String, String> paramsMap) throws Exception;
}
