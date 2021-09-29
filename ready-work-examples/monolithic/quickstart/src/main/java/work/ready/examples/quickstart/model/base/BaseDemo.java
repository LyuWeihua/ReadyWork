package work.ready.examples.quickstart.model.base;

import work.ready.core.database.Bean;
import work.ready.core.database.Model;

/**
 * Generated by Ready.Work
 */
@SuppressWarnings("serial")
public abstract class BaseDemo<M extends BaseDemo<M>> extends Model<M> implements Bean {

	public void setId(Integer id) {
		set("id", id);
	}

	public Integer getId() {
		return getInt("id");
	}

	public void setName(String name) {
		set("name", name);
	}

	public String getName() {
		return getStr("name");
	}

	public void setGender(Integer gender) {
		set("gender", gender);
	}

	public Integer getGender() {
		return getInt("gender");
	}

	public void setAge(Integer age) {
		set("age", age);
	}

	public Integer getAge() {
		return getInt("age");
	}

	public void setHeight(Integer height) {
		set("height", height);
	}

	public Integer getHeight() {
		return getInt("height");
	}

	public void setWeight(Integer weight) {
		set("weight", weight);
	}

	public Integer getWeight() {
		return getInt("weight");
	}

	public void setHobbies(String hobbies) {
		set("hobbies", hobbies);
	}

	public String getHobbies() {
		return getStr("hobbies");
	}

	public void setCreated(java.util.Date created) {
		set("created", created);
	}

	public java.util.Date getCreated() {
		return get("created");
	}

	public void setModified(java.util.Date modified) {
		set("modified", modified);
	}

	public java.util.Date getModified() {
		return get("modified");
	}

	public void setStatus(Integer status) {
		set("status", status);
	}

	public Integer getStatus() {
		return getInt("status");
	}

	public void setIsDeleted(Boolean isDeleted) {
		set("isDeleted", isDeleted);
	}

	public Boolean getIsDeleted() {
		return get("isDeleted");
	}

	public void setVersion(Integer version) {
		set("version", version);
	}

	public Integer getVersion() {
		return getInt("version");
	}

}
