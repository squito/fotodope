package us.therashids.PictureDB;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.therashids.TagDag.Tag;

public class PeopleGroup implements Tag {
	
	Set<Person> people;	//only the immediately contained people
	Set<PeopleGroup> subgroups;
	Set<PeopleGroup> parentgroups;
	String name;
	String desc;
	int id;
	
	public PeopleGroup(){
		id = -1;
		people = new HashSet<Person>();
		subgroups = new HashSet<PeopleGroup>();
		parentgroups = new HashSet<PeopleGroup>();
	}
	
	public PeopleGroup getFromDB(int id) throws SQLException {
		return DBConnection.getInstance().getPeopleGroupFromId(id);
	}
	
	public void getOrAddToDB() throws SQLException {
		DBConnection.getInstance().getOrAddGroup(this);
	}
	
	public void addTagToPicture(PictureInfo pic) throws SQLException {
		DBConnection.getInstance().addGroupInPicture(this, pic);
	}
	
	public Set<PictureInfo> getPictures() throws SQLException {
		Set<Person> allPeople = getAllPeople();
		Set<PictureInfo> allPictures = new HashSet<PictureInfo>();
		allPictures.addAll(DBConnection.getInstance().getPicturesWithPeopleGroup(this));
		for(Person p: allPeople){
			allPictures.addAll(p.getPictures());
		}
		return allPictures;
	}
	
	public Set<Person> getAllPeople(){
		Set<Person> allPeople = new HashSet<Person>();
		getAllPeopleHelper(allPeople);
		return allPeople;
	}
	
	protected void getAllPeopleHelper(Set<Person> soFar){
		soFar.addAll(people);
		for(PeopleGroup s: subgroups){
			getAllPeopleHelper(soFar);
		}
	}
	
	public String getDesc() {
		return desc;
	}
	public void setDesc(String desc) {
		this.desc = desc;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Set<Person> getPeople() {
		return people;
	}
	public void setPeople(Set<Person> people) {
		this.people = people;
	}
	
	public void addPerson(Person p){
		people.add(p);
	}
	
	public Set<PeopleGroup> getSubgroups() {
		return subgroups;
	}
	
	public void setSubGroups(Set<PeopleGroup> subGroups) {
		this.subgroups = subGroups;
	}
	
	public void addSubgroup(PeopleGroup subgroup){
		subgroups.add(subgroup);
	}
	
	public void addParentgroup(PeopleGroup parentgroup){
		parentgroups.add(parentgroup);
	}
	
	public boolean inDB(){
		return id == -1;
	}
	
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof PeopleGroup){
			PeopleGroup g = (PeopleGroup)obj;
			if(inDB())
				return id == g.getId();
			else
				return name.equals(g.getName());
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return name.hashCode();
	}
	
	public Collection<? extends Tag> getChildTags() {
		HashSet<Tag> children = new HashSet<Tag>();
		children.addAll(people);
		children.addAll(subgroups);
		return children;
	}
	
	public Collection<? extends Tag> getParentTags() {
		return parentgroups;
	}
	
	public String toString() {
		return name;
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
