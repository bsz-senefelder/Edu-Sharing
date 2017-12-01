package org.edu_sharing.restservices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.alfresco.repo.search.impl.solr.facet.Exceptions.IllegalArgument;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.security.NoSuchPersonException;
import org.alfresco.service.cmr.security.PersonService;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.authentication.Context;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.shared.Authority;
import org.edu_sharing.restservices.shared.NodeRef;
import org.edu_sharing.restservices.shared.User;
import org.edu_sharing.restservices.shared.UserProfile;
import org.edu_sharing.restservices.shared.UserSimple;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PersonDao {

	public static final String ME = "-me-";
	
	public static PersonDao getPerson(RepositoryDao repoDao, String userName) throws DAOException {
		
		try {
			String currentUser = AuthenticationUtil.getFullyAuthenticatedUser(); 
			
			if (ME.equals(userName)) {
	
				userName = currentUser;
			}
	
			if (   !currentUser.equals(userName) 
				&& !repoDao.getBaseClient().isAdmin(currentUser)
				&& !AuthenticationUtil.isRunAsUserTheSystemUser()
					) {
								
				throw new AccessDeniedException(currentUser);
			}
				
			return new PersonDao(repoDao, userName);
			
		} catch (Exception e) {

			throw DAOException.mapping(e);
		}
	}

	public static void createPerson(RepositoryDao repoDao, String userName,String password, UserProfile profile) throws DAOException {
		
		try {

			try {
				
				repoDao.getBaseClient().getUserInfo(userName);

				throw new DAOValidationException(
						new IllegalArgumentException("Username already exists."));
				
			} catch (NoSuchPersonException e) {
				
				HashMap<String, String> userInfo = new HashMap<String, String>();
				userInfo.put(CCConstants.PROP_USERNAME, userName);
				userInfo.put(CCConstants.PROP_USER_FIRSTNAME, profile.getFirstName());
				userInfo.put(CCConstants.PROP_USER_LASTNAME, profile.getLastName());
				userInfo.put(CCConstants.PROP_USER_EMAIL, profile.getEmail());
				
				((MCAlfrescoAPIClient)repoDao.getBaseClient()).createOrUpdateUser(userInfo);
				if(password!=null)
					new PersonDao(repoDao, userName).changePassword(null,password);
			}			
			
		} catch (Exception e) {

			throw DAOException.mapping(e);
		}
	}
	
	private final MCAlfrescoBaseClient baseClient;

	private final RepositoryDao repoDao;
	
	private final HashMap<String, String> userInfo;
	private final String homeFolderId;
	private final List<String> sharedFolderIds = new ArrayList<String>();
	
	public PersonDao(RepositoryDao repoDao, String userName) throws DAOException  {

		try {
			
			this.baseClient = repoDao.getBaseClient();
	
			this.repoDao = repoDao;

			this.userInfo = baseClient.getUserInfo(userName);
			this.homeFolderId = baseClient.getHomeFolderID(userName);
			try{
				
				boolean getGroupFolder = true;
				//don't run into access denied wrapped by Transaction commit failed
				if(!AuthenticationUtil.isRunAsUserTheSystemUser() 
						&& !AuthenticationUtil.getRunAsUser().equals(ApplicationInfoList.getHomeRepository().getUsername())
						&& !AuthenticationUtil.getRunAsUser().equals(userName)) {
					getGroupFolder = false;
				}
				if(getGroupFolder) {
					String groupFolderId = ((MCAlfrescoAPIClient)baseClient).getGroupFolderId(userName);
					if (groupFolderId != null) {
						
						HashMap<String, HashMap<String, Object>> children = baseClient.getChildren(groupFolderId);
						
						for (Object key : children.keySet()) {
		
							sharedFolderIds.add(key.toString());
						}				
					}
				}
			}catch(InvalidNodeRefException e){
				
			}
			catch(AccessDeniedException e){
			
			}
			
		} catch (Throwable t) {
			t.printStackTrace();
			throw DAOException.mapping(t);
		}
	}
	
	public void changeProfile(UserProfile profile) throws DAOException {
		
		try {

			HashMap<String, String> newUserInfo = new HashMap<String, String>();
			
			newUserInfo.put(CCConstants.PROP_USERNAME, getUserName());
			
			newUserInfo.put(CCConstants.PROP_USER_FIRSTNAME, profile.getFirstName());
			newUserInfo.put(CCConstants.PROP_USER_LASTNAME, profile.getLastName());
			newUserInfo.put(CCConstants.PROP_USER_EMAIL, profile.getEmail());
			
			((MCAlfrescoAPIClient)this.baseClient).createOrUpdateUser(newUserInfo);
			
		} catch (Throwable t) {
			
			throw DAOException.mapping(t);
		}

	}
	
	public void changePassword(String oldPassword, String newPassword) throws DAOException {
		
		try {
			
			if (oldPassword == null) {
			
				((MCAlfrescoAPIClient)this.baseClient).setUserPassword(getUserName(), newPassword);
				
			} else {

				((MCAlfrescoAPIClient)this.baseClient).updateUserPassword(getUserName(), oldPassword, newPassword);
				
			}
				
		} catch (Throwable t) {
			
			throw DAOException.mapping(t);
		}

	}
	
	public void delete() throws DAOException {
		
		try {

			String currentUser = AuthenticationUtil.getFullyAuthenticatedUser(); 
			
			if (currentUser.equals(getUserName())) {
								
				throw new DAOValidationException(
						new IllegalArgumentException("Session user can not be deleted."));
			}

			((MCAlfrescoAPIClient)this.baseClient).deleteUser(getUserName());
			
		} catch (Exception e) {

			throw DAOException.mapping(e);
		}
	}


	public User asPerson() {
		
    	User data = new User();
    	
    	data.setAuthorityName(getAuthorityName());
    	data.setAuthorityType(Authority.Type.USER);
    	
    	data.setUserName(getUserName());
    	
    	UserProfile profile = new UserProfile();
    	profile.setFirstName(getFirstName());
    	profile.setLastName(getLastName());
    	profile.setEmail(getEmail());
    	data.setProfile(profile);
    	
    	NodeRef homeDir = new NodeRef();
    	homeDir.setRepo(repoDao.getId());
    	homeDir.setId(getHomeFolder());
    	data.setHomeFolder(homeDir);

    	List<NodeRef> sharedFolderRefs = new ArrayList<NodeRef>();
    	for (String sharedFolderId : sharedFolderIds) {
    		
        	NodeRef sharedFolderRef = new NodeRef();
        	sharedFolderRef.setRepo(repoDao.getId());
        	sharedFolderRef.setId(sharedFolderId);
        	
        	sharedFolderRefs.add(sharedFolderRef);	
    	}
    	data.setSharedFolders(sharedFolderRefs);

    	return data;
	}
	public UserSimple asPersonSimple() {
		UserSimple data = new UserSimple();    	
    	data.setAuthorityName(getAuthorityName());
    	data.setAuthorityType(Authority.Type.USER);    	
    	data.setUserName(getUserName());    	
    	UserProfile profile = new UserProfile();
    	profile.setFirstName(getFirstName());
    	profile.setLastName(getLastName());
    	profile.setEmail(getEmail());
    	data.setProfile(profile);
    	
    	NodeRef homeDir = new NodeRef();
    	homeDir.setRepo(repoDao.getId());
    	homeDir.setId(getHomeFolder());
    	return data;
	}
	public String getId() {
		
		return this.userInfo.get(CCConstants.SYS_PROP_NODE_UID);
	}
	
	public String getAuthorityName() {
		
		return getUserName();
	}
	
	public String getUserName() {
		
		return this.userInfo.get(CCConstants.CM_PROP_PERSON_USERNAME);
	}
	
	public String getFirstName() {
		
		return this.userInfo.get(CCConstants.CM_PROP_PERSON_FIRSTNAME);
	}
	
	public String getLastName() {
		
		return this.userInfo.get(CCConstants.CM_PROP_PERSON_LASTNAME);
	}
	
	public String getEmail() {
		
		return this.userInfo.get(CCConstants.CM_PROP_PERSON_EMAIL);
	}
	
	public String getHomeFolder() {
		
		return this.homeFolderId;
	}

	public String getPreferences() {
		return this.userInfo.get(CCConstants.CCM_PROP_PERSON_PREFERENCES);
	}
	public void setPreferences(String preferences) throws Exception{
		// validate json
		new JSONObject(preferences);
		HashMap<String, String> newUserInfo = new HashMap<String, String>();
		newUserInfo.put(CCConstants.PROP_USERNAME, getUserName());		
		newUserInfo.put(CCConstants.CCM_PROP_PERSON_PREFERENCES, preferences);		
		((MCAlfrescoAPIClient)this.baseClient).updateUser(newUserInfo);
	}
	public void addNodeList(String list,String nodeId) throws Exception {
		// Simply check if node is valid
		NodeDao node=NodeDao.getNode(repoDao, nodeId);
		if(node.isDirectory())
			throw new IllegalArgumentException("The node "+nodeId+" is a directory. Only files are allowed for this list");
		String data=getCurrentNodeListJson();
		JSONObject json=new JSONObject();
		if(data!=null)
			json=new JSONObject(data);
		
		JSONArray array=null;
		if(json.has(list))
			array=json.getJSONArray(list);
		List<JSONObject> nodes=new ArrayList<>();
		if(array!=null){
			for(int i=0;i<array.length();i++){
				if(array.getJSONObject(i).getString("id").equals(nodeId))
					throw new IllegalAccessException("Node is already in list: "+nodeId);
				nodes.add(array.getJSONObject(i));
			}
		}
		JSONObject object=new JSONObject();
		object.put("id",nodeId);
		object.put("dateAdded",System.currentTimeMillis());
		nodes.add(object);
		json.put(list,new JSONArray(nodes));
		updateNodeList(json);
	}
	private String getCurrentNodeListJson(){
		org.edu_sharing.service.authority.AuthorityService service=AuthorityServiceFactory.getAuthorityService(ApplicationInfoList.getHomeRepository().getAppId());
		HttpSession session = Context.getCurrentInstance().getRequest().getSession();
		String data;
		if(service.isGuest()){
			data=(String) session.getAttribute(CCConstants.CCM_PROP_PERSON_NODE_LISTS);
		}
		else{
			data=this.userInfo.get(CCConstants.CCM_PROP_PERSON_NODE_LISTS);
		}
		return data;
	}
	public List<NodeRef> getNodeList(String list) throws Exception {
		String data=getCurrentNodeListJson();
		if(data==null)
			return null;
		JSONObject json=new JSONObject(data);
		if(!json.has(list))
			return null;
		JSONArray array=json.getJSONArray(list);
		List<NodeRef> result=new ArrayList<>();
		for(int i=0;i<array.length();i++){
			String nodeId=array.getJSONObject(i).getString("id");
			try{
				// causes invalid nodes to fire throwable -> delete them
				NodeDao.getNode(repoDao, nodeId);
				result.add(new NodeRef(repoDao.getId(), nodeId));
			}
			catch(Throwable t){
				removeNodeList(list,nodeId);
			}
		}
		return result;			
	}

	public void removeNodeList(String list, String nodeId) throws Exception {
		String data=getCurrentNodeListJson();
		if(data==null)
			throw new IllegalArgumentException("Node list not found: "+list);
		JSONObject json=new JSONObject(data);
		if(!json.has(list))
			throw new IllegalArgumentException("Node list not found: "+list);
		JSONArray array=json.getJSONArray(list);
		boolean found=false;
		List<JSONObject> result=new ArrayList<>();
		for(int i=0;i<array.length();i++){
			if(array.getJSONObject(i).getString("id").equals(nodeId)){
				found=true;
			}
			else
				result.add(array.getJSONObject(i));
		}
		if(!found)
			throw new IllegalArgumentException("Node not found in list: "+nodeId);
		json.put(list, new JSONArray(result));
		updateNodeList(json);
}

	private void updateNodeList(JSONObject json) throws Exception {
		org.edu_sharing.service.authority.AuthorityService service=AuthorityServiceFactory.getAuthorityService(ApplicationInfoList.getHomeRepository().getAppId());
		HttpSession session = Context.getCurrentInstance().getRequest().getSession();
		if(service.isGuest()){
			session.setAttribute(CCConstants.CCM_PROP_PERSON_NODE_LISTS,json.toString());
		}
		else{
			HashMap<String, String> newUserInfo = new HashMap<String, String>();
			newUserInfo.put(CCConstants.PROP_USERNAME, getUserName());		
			newUserInfo.put(CCConstants.CCM_PROP_PERSON_NODE_LISTS, json.toString());		
			((MCAlfrescoAPIClient)this.baseClient).updateUser(newUserInfo);
		}
	}
}
