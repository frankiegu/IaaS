package com.appcloud.vm.action.image;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.log4j.Logger;

import appcloud.openapi.client.ImageClient;
import appcloud.openapi.response.BaseResponse;

import com.appcloud.vm.action.BaseAction;
import com.appcloud.vm.fe.common.Constants;
import com.appcloud.vm.fe.manager.AppkeyManager;
import com.appcloud.vm.fe.model.Appkey;
import com.appcloud.vm.fe.util.OpenClientFactory;

public class DeleteImageAction extends BaseAction {
	/**
	 * 删除云镜像
	 */
	private static final long serialVersionUID = 1L;
	private Logger logger = Logger.getLogger(DeleteImageAction.class);
	private ImageClient imageClient = OpenClientFactory.getImageClient();
	private AppkeyManager appkeyManager = new AppkeyManager();

	//删除镜像的参数
	private String providerEn;//提供商
	private String userEmail;//邮箱
	private String regionId;//地域Id
	private String imageId;//镜像Id
	private String result = "0";//结果参数，0:正确，1:参数出错

	public String execute(){
		logger.info(toString());
		switch (providerEn.trim()) {
		case Constants.YUN_HAI:
			DeleteYunhaiImage();
			break;
		case Constants.ALI_YUN:
			DeleteAliyunImage();
			break;
		case Constants.AMAZON:
			DeleteAmozonImage();
			break;
		default:
			break;
		}
		return SUCCESS;
	}

	public void DeleteYunhaiImage(){
		Appkey appkey = appkeyManager.getAppkeyByUserEmail(userEmail.trim());
		BaseResponse deleteImageResponse = imageClient.DeleteImage(regionId.trim(), null, imageId.trim(), appkey.getAppkeyId(), appkey.getAppkeySecret(), userEmail.trim());
		logger.info(ToStringBuilder.reflectionToString(deleteImageResponse));
		if (null!=deleteImageResponse.getErrorCode()) {
			result = "1";
		}
	}

	public void DeleteAliyunImage() {}

	public void DeleteAmozonImage() {}

	public String getProviderEn() {
		return providerEn;
	}

	public void setProviderEn(String providerEn) {
		this.providerEn = providerEn;
	}

	public String getUserEmail() {
		return userEmail;
	}

	public void setUserEmail(String userEmail) {
		this.userEmail = userEmail;
	}

	public String getRegionId() {
		return regionId;
	}

	public void setRegionId(String regionId) {
		this.regionId = regionId;
	}

	public String getImageId() {
		return imageId;
	}

	public void setImageId(String imageId) {
		this.imageId = imageId;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	@Override
	public String toString() {
		return "DeleteImageAction [providerEn=" + providerEn + ", userEmail="
				+ userEmail + ", regionId=" + regionId + ", imageId=" + imageId
				+ ", result=" + result + "]";
	}


}
