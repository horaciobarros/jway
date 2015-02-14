package jway;

public class CampoFk {
	
	private String fkTableName;
	private String fkColumnName;
	private String pkTableName;
	private String pkColumnName;
	public String getFkTableName() {
		return fkTableName;
	}
	public void setFkTableName(String fkTableName) {
		this.fkTableName = fkTableName;
	}
	public String getFkColumnName() {
		return fkColumnName;
	}
	public void setFkColumnName(String fkColumnName) {
		this.fkColumnName = fkColumnName;
	}
	public String getPkTableName() {
		return pkTableName;
	}
	public void setPkTableName(String pkTableName) {
		this.pkTableName = pkTableName;
	}
	public String getPkColumnName() {
		return pkColumnName;
	}
	public void setPkColumnName(String pkColumnName) {
		this.pkColumnName = pkColumnName;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((fkColumnName == null) ? 0 : fkColumnName.hashCode());
		result = prime * result
				+ ((fkTableName == null) ? 0 : fkTableName.hashCode());
		result = prime * result
				+ ((pkColumnName == null) ? 0 : pkColumnName.hashCode());
		result = prime * result
				+ ((pkTableName == null) ? 0 : pkTableName.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CampoFk other = (CampoFk) obj;
		if (fkColumnName == null) {
			if (other.fkColumnName != null)
				return false;
		} else if (!fkColumnName.equals(other.fkColumnName))
			return false;
		if (fkTableName == null) {
			if (other.fkTableName != null)
				return false;
		} else if (!fkTableName.equals(other.fkTableName))
			return false;
		if (pkColumnName == null) {
			if (other.pkColumnName != null)
				return false;
		} else if (!pkColumnName.equals(other.pkColumnName))
			return false;
		if (pkTableName == null) {
			if (other.pkTableName != null)
				return false;
		} else if (!pkTableName.equals(other.pkTableName))
			return false;
		return true;
	}
	
	

}
