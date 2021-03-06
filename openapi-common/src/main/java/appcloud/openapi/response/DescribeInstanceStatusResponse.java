package appcloud.openapi.response;

import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import appcloud.openapi.constant.ResultConstants;
import appcloud.openapi.datatype.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "DescribeInstanceStatusResponse")
public class DescribeInstanceStatusResponse {
	
	/**
	 * 直接把参数名命名为子节点的名字，然后统一使用
	 * @XmlAccessorType(XmlAccessType.FIELD)进行注解，省去单独为其设置子节点名字的麻烦
	 * 除此之外，使用@XmlAccessorType(XmlAccessType.FIELD)注解，可以使子节点名的显示顺序与
	 * 参数声明的顺序一致
	 */
	private String RequestId;
	private Integer TotalCount;
	private Integer PageNumber;
	private Integer PageSize;
	private InstanceStatusSet InstanceStatuses;  
	private String ErrorCode;
	private String Message;
	
	protected DescribeInstanceStatusResponse() {
	}

	public DescribeInstanceStatusResponse(String requestId, Map<String, Object> resultMap) {
		RequestId = requestId;
		TotalCount = Integer.parseInt(resultMap.get(ResultConstants.TOTAL_COUNT).toString());
		PageNumber = Integer.parseInt(resultMap.get(ResultConstants.PAGE_NUMBER).toString());
		PageSize = Integer.parseInt(resultMap.get(ResultConstants.PAGE_SIZE).toString());
		InstanceStatuses = (InstanceStatusSet) resultMap.get(ResultConstants.INSTANCE_STATUS_SET);
	}
	
	public DescribeInstanceStatusResponse(String requestId, String errorCode, String message) {
		RequestId = requestId;
		ErrorCode = errorCode;
		Message = message;
	}

	public String getRequestId() {
		return RequestId;
	}
	public void setRequestId(String requestId) {
		RequestId = requestId;
	}

	public String getErrorCode() {
		return ErrorCode;
	}
	public void setErrorCode(String errorCode) {
		ErrorCode = errorCode;
	}

	public String getMessage() {
		return Message;
	}
	public void setMessage(String message) {
		Message = message;
	}

	public Integer getTotalCount() {
		return TotalCount;
	}
	public void setTotalCount(Integer totalCount) {
		TotalCount = totalCount;
	}

	public Integer getPageNumber() {
		return PageNumber;
	}
	public void setPageNumber(Integer pageNumber) {
		PageNumber = pageNumber;
	}

	public Integer getPageSize() {
		return PageSize;
	}
	public void setPageSize(Integer pageSize) {
		PageSize = pageSize;
	}
	
	public InstanceStatusSet getInstanceStatuses() {
		return InstanceStatuses;
	}

	public void setInstanceStatuses(InstanceStatusSet instanceStatuses) {
		InstanceStatuses = instanceStatuses;
	}

}
