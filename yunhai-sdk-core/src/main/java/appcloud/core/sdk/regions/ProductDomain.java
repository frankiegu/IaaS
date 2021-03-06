package appcloud.core.sdk.regions;

public class ProductDomain {

	private String productName;
	private String zoneId;
	private String domianName;

	public ProductDomain(String product,String zoneId, String domain) {
		this.productName = product;
		this.zoneId = zoneId;
		this.domianName = domain;
	}

	public String getProductName() {
		return productName;
	}
	public void setProductName(String productName) {
		this.productName = productName;
	}
	public String getZoneId() {
		return zoneId;
	}
	public void setZoneId(String zoneId) {
		this.zoneId = zoneId;
	}
	public String getDomianName() {
		return domianName;
	}
	public void setDomianName(String domianName) {
		this.domianName = domianName;
	}

	@Override
	public String toString() {
		return "ProductDomain{" +
				"productName='" + productName + '\'' +
				", zoneId='" + zoneId + '\'' +
				", domianName='" + domianName + '\'' +
				'}';
	}
}
