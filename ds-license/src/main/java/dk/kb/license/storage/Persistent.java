package dk.kb.license.storage;


/**
 * Superclass for all persistent DTO's. 
 * 
 * All DTO has an 'id' field in the database. The ID must be unique for each instance of that DTO type.
 * 
 */
public abstract class Persistent {
	protected long id;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
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
		Persistent other = (Persistent) obj;
		if (id != other.id)
			return false;
		return true;
	}

	
	
}
