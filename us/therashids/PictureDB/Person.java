package us.therashids.PictureDB;

import java.sql.SQLException;
import java.util.*;

import com.therashids.TagDag.Tag;

public class Person implements Tag {
	
	String firstName;
	String lastName;
	List<String> aliases;
	int id;
	
	/**
	 * take a string that is a space-seperated first and last name.
	 * @param fullName
	 */
	public Person(String fullName){
		String[] p = fullName.split(" ");
		firstName = p[0];
		if(p.length == 1)
			lastName = "";
		else
			lastName = p[1];
		id = -1;
	}
	
	public Person getFromDB(int id) throws SQLException {
		return DBConnection.getInstance().getPersonFromId(id);
	}
	
	public void getOrAddToDB() throws SQLException {
		DBConnection.getInstance().getOrAddPerson(this);
	}
	
	public void addTagToPicture(PictureInfo pic) throws SQLException {
		DBConnection.getInstance().addPersonInPicture(this, pic);
	}
	
	public List<PictureInfo> getPictures() throws SQLException {
		return DBConnection.getInstance().getPicturesWithPeople(this);
	}
	
	public void addAlias(String alias){
		if(aliases == null)
			aliases = new ArrayList<String>();
		aliases.add(alias);
	}
	
	public List<String> getAliases() {
		return aliases;
	}
	public void setAliases(List<String> aliases) {
		this.aliases = aliases;
	}
	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	
	@Override
	public String toString() {
		return getFullName();
	}
	
	public String getFullName(){
		if(lastName.equals(""))
			return firstName;
		return firstName + " " + lastName;
	}
	
	public boolean inDB(){
		return id == -1;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Person){
			Person p = (Person)obj;
			if(inDB())
				return id == p.getId();
			else
				return getFullName().equals(p.getFullName());
		}
		return false;
	}

	
	@Override
	public int hashCode() {
		return getFullName().hashCode();
	}
	
	public Collection<? extends Tag> getChildTags() {
		return null;
	}
	
	public Collection<? extends Tag> getParentTags() {
		return null;
	}

	public void addChild(Tag t) {
		// TODO Auto-generated method stub
		
	}

	public void addParent(Tag t) {
		// TODO Auto-generated method stub
		
	}

	public Collection<Class<? extends Tag>> getAllowedSubtagTypes() {
		// TODO Auto-generated method stub
		return null;
	}

	public Tag getChildTag(int n) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getClassDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getKey() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getLongForm() {
		// TODO Auto-generated method stub
		return null;
	}

	public Tag getTag(String key) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isRootLevel() {
		// TODO Auto-generated method stub
		return false;
	}

	public void setRootLevel(boolean rootLevel) {
		// TODO Auto-generated method stub
		
	}

	public void sortChildren() {
		// TODO Auto-generated method stub
		
	}
	
	public Set<? extends Tag> getDescendantTags() {
		// TODO Auto-generated method stub
		return null;
	}

}
