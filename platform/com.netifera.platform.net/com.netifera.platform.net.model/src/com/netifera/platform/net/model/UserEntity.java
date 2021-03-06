package com.netifera.platform.net.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.netifera.platform.api.model.AbstractEntity;
import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.IEntityReference;
import com.netifera.platform.api.model.IWorkspace;
import com.netifera.platform.util.addresses.inet.InternetAddress;

public class UserEntity extends AbstractEntity {
		//implements Comparable<UserEntity> {
	
	private static final long serialVersionUID = 1049344731407259618L;

	public final static String ENTITY_NAME = "user";

	private final IEntityReference host;
	private final String name;
	private Map<String,String> hashes;
	private boolean locked = false;

	public UserEntity(IWorkspace workspace, HostEntity host, String name) {
		super(ENTITY_NAME, workspace, host.getRealmId());
		this.host = host.createReference();
		this.name = name;
		this.hashes = new HashMap<String,String>();
	}

	UserEntity() {
		this.host = null;
		this.name = null;
	}
	
	public HostEntity getHost() {
		return (HostEntity) referenceToEntity(host);
	}

	public String getName() {
		return name;
	}

	public void setPassword(String password) {
		setAttribute("password", password);
	}

	public String getPassword() {
		return getAttribute("password");
	}

	public void setHash(String hashType, String hash) {
		hashes.put(hashType, hash);
	}

	public Set<String> getHashTypes() {
		return Collections.unmodifiableSet(hashes.keySet());
	}
	
	public String getHash(String hashType) {
		return hashes.get(hashType);
	}

	public void setHome(String home) {
		setAttribute("home", home);
	}

	public String getHome() {
		return getAttribute("home");
	}
	
	public void setLocked(boolean locked) {
		this.locked = locked;
	}
	
	public boolean isLocked() {
		return locked;
	}
	
	public boolean isPriviledged() {
		return name.equals("root") || name.toLowerCase().equals("administrator"); //XXX HACK
	}
	
	@Override
	protected void synchronizeEntity(AbstractEntity masterEntity) {
		hashes = ((UserEntity)masterEntity).hashes;
		locked = ((UserEntity)masterEntity).locked;
	}

	@Override
	protected IEntity cloneEntity() {
		UserEntity clone = new UserEntity(getWorkspace(), getHost(), getName());
		clone.hashes = hashes;
		clone.locked = locked;
		return clone;
	}
	
	public static String createQueryKey(long realmId, String name, long hostId) {
		return ENTITY_NAME + ":" + realmId + ":" + name + ":" + hostId;
	}
	
	@Override
	protected String generateQueryKey() {
		return createQueryKey(getRealmId(), name, getHost().getId());
	}
	
	public int compareTo(UserEntity other) {
		int r = name.compareTo(other.name);
		return r > 0 ? 1 : (r < 0 ? -1 : 0);
	}
	
	public static synchronized UserEntity create(IWorkspace workspace, long realm, long spaceId, InternetAddress address, String username) {
		HostEntity hostEntity = InternetAddressEntity.create(workspace, realm, spaceId, address).getHost();
		
		UserEntity userEntity = (UserEntity) workspace.findByKey(createQueryKey(realm, username, hostEntity.getId()));
		if(userEntity != null) {
			userEntity.addToSpace(spaceId);
			return userEntity;
		}
		
		userEntity = new UserEntity(workspace, hostEntity, username);
		userEntity.save();
		userEntity.addToSpace(spaceId);
		return userEntity;
	}
}
