package com.netifera.platform.ui.spaces.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.IShadowEntity;
import com.netifera.platform.api.model.layers.IGroupLayer;
import com.netifera.platform.api.model.layers.ISemanticLayer;
import com.netifera.platform.api.model.layers.ITreeLayer;
import com.netifera.platform.model.FolderEntity;
import com.netifera.platform.model.TreeStructureContext;

public class TreeBuilder {
	private volatile IShadowEntity root;
	private final List<ITreeLayer> treeProviders;
	private final List<IGroupLayer> groupProviders;
	private Map<Long,List<IShadowEntity>> shadowsMap;
	private Map<Long,Map<String,IShadowEntity>> foldersMap;
	
	private ITreeBuilderListener updateListener;
	
	public TreeBuilder(List<ISemanticLayer> providers) {
		treeProviders = new ArrayList<ITreeLayer>();
		groupProviders = new ArrayList<IGroupLayer>();
		for (ISemanticLayer layerProvider: providers) {
			addLayer(layerProvider);
		}
	}

	public synchronized List<ISemanticLayer> getLayers() {
		List<ISemanticLayer> answer = new ArrayList<ISemanticLayer>();
		answer.addAll(treeProviders);
		answer.addAll(groupProviders);
		return answer;
	}
	
	public synchronized void addLayer(ISemanticLayer layerProvider) {
		if (layerProvider instanceof ITreeLayer) {
			treeProviders.add((ITreeLayer) layerProvider);
		} else if (layerProvider instanceof IGroupLayer) {
			groupProviders.add((IGroupLayer) layerProvider);
		}
	}
	
	public synchronized void removeLayer(ISemanticLayer layerProvider) {
		if (!treeProviders.remove(layerProvider))
			groupProviders.remove(layerProvider);
	}

	public synchronized void setListener(ITreeBuilderListener listener) {
		updateListener = listener;
	}
	
	public synchronized void setRoot(final IEntity rootEntity) {
		dispose();
		shadowsMap = new HashMap<Long,List<IShadowEntity>>();
		foldersMap = new HashMap<Long,Map<String,IShadowEntity>>();
		root = handleRootEntity(rootEntity);
	}
	
	public IShadowEntity getRoot() {
		return root;
	}
	
	public synchronized void addEntity(IEntity entity) {
		if (getShadow(entity.getId()) != null) return;

		//NOTE this might be wrong, we're adding the entity under a shadow of the realm, but there might be many shadows of the realm
//		IShadowEntity realmEntity = treeRoot.getStructureContext().searchEntityById(entity.getRealmId());
		IShadowEntity realmEntity = getShadow(entity.getRealmId());
		if(realmEntity == null) {
			System.err.println("Realm entity not found for entity: "  + entity);
			realmEntity = root;
		}

		handleEntity(entity, realmEntity);
	}
	
	private IShadowEntity handleRootEntity(IEntity entity) {
		IShadowEntity shadow = TreeStructureContext.createRoot(entity);
		List<IShadowEntity> list = new ArrayList<IShadowEntity>();
		list.add(shadow);
		shadowsMap.put(entity.getId(), list);
		return shadow;
	}

	private void handleEntity(IEntity entity, IShadowEntity realmEntity) {
		Set<String> groups = getGroups(entity);
		for (String group: groups) {
			IShadowEntity folder = createFolder(group, realmEntity);
			addChildEntity(entity, folder);
		}
	
		for (ITreeLayer provider: treeProviders) {
			if (provider.isRealmRoot(entity) && groups.size() == 0) {
				// if it's not in any folder, add it to the realm root
				addChildEntity(entity, realmEntity);
			} else {
				for (IEntity parent: provider.getParents(entity))
					for (IShadowEntity parentShadow: getShadows(parent.getId()))
						addChildEntity(entity, parentShadow);
			}
		}
	}

	public synchronized void updateEntity(IEntity entity) {
		IShadowEntity shadow = getShadow(entity.getId());
		if (shadow == null) return;

		IShadowEntity realmEntity = getShadow(entity.getRealmId());
		if(realmEntity == null) {
			System.err.println("Realm entity not found for entity: " + entity);
			realmEntity = root;
		}

		// if some group was removed, make sure the entity is not included in the folder
		List<IShadowEntity> shadows = getShadows(entity.getId());
		Set<String> groups = getGroups(entity);
		for (IShadowEntity child: shadows.toArray(new IShadowEntity[shadows.size()])) {
			IShadowEntity parent = ((TreeStructureContext)child.getStructureContext()).getParent();
			if (parent instanceof FolderEntity) {
				FolderEntity folder = (FolderEntity) parent;
				if (!groups.contains(folder.getTag())) {
					removeChildEntity(entity, parent);
				}
			}
		}

		// make sure the entity is included in a folder for each of its groups
		if (groups.size() > 0) {
			for (String group: groups) {
				IShadowEntity folder = createFolder(group, realmEntity);
				if (!((TreeStructureContext)folder.getStructureContext()).hasChild(entity))
					copySubtree(shadow, folder);
			}
			removeChildEntity(entity, realmEntity);
		}

		for (IShadowEntity each: getShadows(entity.getId())) {
			updateListener.entityChanged(each);
		}

		if (getShadow(entity.getId()) == null) {
			handleEntity(entity, realmEntity);
		}
				
		return;
	}

	public synchronized void removeEntity(IEntity entity) {
		Set<IShadowEntity> parents = new HashSet<IShadowEntity>();

		for (IShadowEntity shadow: getShadows(entity.getId())) {
			TreeStructureContext context = (TreeStructureContext) shadow.getStructureContext();
			if (context.getParent() != null)
				parents.add(context.getParent());
		}
		
		for (IShadowEntity parent: parents)
			removeChildEntity(entity, parent);

		return;
	}

	private Set<String> getGroups(IEntity entity) {
		Set<String> answer = new HashSet<String>();
		for (IGroupLayer provider: groupProviders)
			answer.addAll(provider.getGroups(entity));
		return answer;
	}

	public List<IShadowEntity> getShadows(long id) {
		List<IShadowEntity> shadows = shadowsMap.get(id);
		if (shadows != null)
			return shadows;
		return Collections.emptyList();
	}

	public IShadowEntity getShadow(long id) {
		List<IShadowEntity> shadows = shadowsMap.get(id);
		if (shadows != null && shadows.size() > 0)
			return shadows.get(0);
		return null;
	}

	private IShadowEntity createFolder(String tag, IShadowEntity realmEntity) {
		Map<String,IShadowEntity> folderMap2 = foldersMap.get(realmEntity.getId());
		if (folderMap2 == null) {
			folderMap2 = new HashMap<String,IShadowEntity>();
			foldersMap.put(realmEntity.getId(), folderMap2);
		}
		IShadowEntity shadow = folderMap2.get(tag);
		if (shadow == null) {
			shadow = addChildEntity(new FolderEntity(realmEntity.getId(), null, tag), realmEntity);
			folderMap2.put(tag, shadow);
		}
		return shadow;
	}
	
	private IShadowEntity addChildEntity(IEntity entity, IShadowEntity parent) {
		IShadowEntity shadow = ((TreeStructureContext) parent.getStructureContext()).addChild(entity);
		if (shadow != null) {
			if (!(entity instanceof FolderEntity)) {
				List<IShadowEntity> shadows = shadowsMap.get(entity.getId());
				if (shadows == null) {
					shadows = new ArrayList<IShadowEntity>();
					shadowsMap.put(entity.getId(), shadows);
				}
				shadows.add(shadow);
			}
			updateListener.entityAdded(shadow, parent);
		}
		return shadow;
	}

	private IShadowEntity removeChildEntity(IEntity entity, IShadowEntity parent) {
		TreeStructureContext tsc = (TreeStructureContext)parent.getStructureContext();
		IShadowEntity shadow = tsc.removeChild(entity);
		if (shadow != null) {
			if (!(entity instanceof FolderEntity)) {
				List<IShadowEntity> shadows = shadowsMap.get(entity.getId());
				shadows.remove(shadow);
				if (shadows.size() == 0)
					shadowsMap.remove(entity.getId());
			}
			updateListener.entityRemoved(shadow, parent);
			
			// if the parent is a folder and becomes empty after removing this child, then remove the folder too
			if (parent instanceof FolderEntity && !tsc.hasChildren()) {
				Map<String,IShadowEntity> folderMap2 = foldersMap.get(parent.getRealmId());
				if (folderMap2 != null)
					folderMap2.remove(((FolderEntity)parent).getTag());
				IShadowEntity grandParent = tsc.getParent();
				if (grandParent != null)
					removeChildEntity(parent, grandParent);
			}
		}
		return shadow;
	}
	
	private IShadowEntity copySubtree(IShadowEntity shadow, IShadowEntity parent) {
		IShadowEntity newShadow = addChildEntity(shadow.getRealEntity(), parent);
		for (IShadowEntity child: ((TreeStructureContext)shadow.getStructureContext()).getChildren()) {
			copySubtree(child, newShadow);
		}
		return newShadow;
	}
	
	public synchronized void dispose() {
		if (shadowsMap == null) return;
		root.dispose(); // will dispose the root shadow and all the children recursively
		shadowsMap = null;
	}
}
