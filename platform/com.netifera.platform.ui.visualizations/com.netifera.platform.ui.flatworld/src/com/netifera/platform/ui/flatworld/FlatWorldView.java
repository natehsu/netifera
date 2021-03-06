package com.netifera.platform.ui.flatworld;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import com.netifera.platform.api.events.IEvent;
import com.netifera.platform.api.events.IEventHandler;
import com.netifera.platform.api.model.AbstractEntity;
import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.ISpace;
import com.netifera.platform.api.model.events.ISpaceContentChangeEvent;
import com.netifera.platform.api.model.layers.IEdge;
import com.netifera.platform.api.model.layers.IEdgeLayer;
import com.netifera.platform.api.model.layers.ISemanticLayer;
import com.netifera.platform.net.ui.geoip.IGeographicalLayer;
import com.netifera.platform.net.ui.geoip.ILocation;
import com.netifera.platform.ui.flatworld.layers.FocusFlatWorldLayer;
import com.netifera.platform.ui.flatworld.layers.SpotsFlatWorldLayer;
import com.netifera.platform.ui.internal.flatworld.Activator;
import com.netifera.platform.ui.spaces.SpaceEditorInput;

public class FlatWorldView extends ViewPart {

	public static final String ID = "com.netifera.platform.views.flatworld";

	private IMemento memento;
	
	private FlatWorld world;
	private FlatWorldUpdater updater;
	private Job loadJob;

	private SpotsFlatWorldLayer<IEntity> spotsLayer;
//	private RaindropFlatWorldLayer raindropsLayer;
	private FocusFlatWorldLayer focusLayer;

	private volatile boolean followNewEntities = false;
	private IEntity focusEntity;

	private List<ISemanticLayer> layers = new ArrayList<ISemanticLayer>();

	private ISpace space;
	
	private IEventHandler spaceChangeListener = new IEventHandler() {
		public void handleEvent(final IEvent event) {
			if(event instanceof ISpaceContentChangeEvent) {
				handleSpaceChange((ISpaceContentChangeEvent)event);
			}
		}
	};

//	private Color red;
	
	@Override
	public void createPartControl(final Composite parent) {
//		red = Display.getDefault().getSystemColor(SWT.COLOR_RED);
		
		for (ISemanticLayer layer: Activator.getInstance().getModel().getSemanticLayers()) {
			if (layer.isDefaultEnabled() &&
					(layer instanceof IGeographicalLayer || layer instanceof IEdgeLayer))
				layers.add(layer);
		}
		
		world = new FlatWorld(parent, SWT.BORDER);
		world.setLayout(new FillLayout());
		
		updater = FlatWorldUpdater.get(world);

		if (memento != null) {
			IMemento worldMemento = memento.getChild("World");
			if (worldMemento != null)
				world.restoreState(worldMemento);
			memento = null;
		}

		IPageListener pageListener = new IPageListener() {
			IPartListener partListener = new IPartListener() {
				public void partActivated(IWorkbenchPart part) {
					if (!(part instanceof IEditorPart))
						return;
					IEditorInput editorInput = ((IEditorPart) part).getEditorInput();
					if (editorInput instanceof SpaceEditorInput) {
						ISpace newSpace = ((SpaceEditorInput)editorInput).getSpace();
						setSpace(newSpace);
					}
				}
				public void partBroughtToTop(IWorkbenchPart part) {
				}
				public void partClosed(IWorkbenchPart part) {
					if (!(part instanceof IEditorPart))
						return;
					IEditorInput editorInput = ((IEditorPart) part).getEditorInput();
					if (editorInput instanceof SpaceEditorInput) {
						ISpace closedSpace = ((SpaceEditorInput)editorInput).getSpace();
						if (closedSpace == FlatWorldView.this.space)
							setSpace(null);
					}
				}
				public void partDeactivated(IWorkbenchPart part) {
				}
				public void partOpened(IWorkbenchPart part) {
				}
			};
			public void pageActivated(IWorkbenchPage page) {
				page.addPartListener(partListener);
				IEditorPart editor = page.getActiveEditor();
				if (editor != null) partListener.partActivated(editor);
			}
			public void pageClosed(IWorkbenchPage page) {
				page.removePartListener(partListener);
			}
			public void pageOpened(IWorkbenchPage page) {
			}
		};
		
		getSite().getWorkbenchWindow().addPageListener(pageListener);
		IWorkbenchPage page = getSite().getWorkbenchWindow().getActivePage();
		if (page != null)
			pageListener.pageActivated(page);

		getSite().getWorkbenchWindow().getSelectionService().addPostSelectionListener(
				//"com.netifera.platform.editors.spaces",
				new ISelectionListener() {
					public void selectionChanged(IWorkbenchPart part, org.eclipse.jface.viewers.ISelection sel) {
						if(sel instanceof IStructuredSelection && !sel.isEmpty()) {
							Object o = ((IStructuredSelection)sel).iterator().next();
							if(o instanceof IEntity) {
								focusEntity((IEntity)o);
							}
						}
					}
					
				});
/*		IWorkbenchPage page = getSite().getWorkbenchWindow().getActivePage();
		if (page == null)
			return;
*/	
	
//		initializeToolBar();
	}

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		this.memento = memento;
	}

	@Override
	public void dispose() {
		if (this.space != null) {
			this.space.removeChangeListener(spaceChangeListener);
			this.loadJob.cancel();
		}
		
		super.dispose();
	}
	
	@Override
	public void saveState(IMemento memento) {
		super.saveState(memento);
		IMemento worldMemento = memento.createChild("World");
		world.saveState(worldMemento);
	}
	
	@Override
	public void setFocus() {
		// TODO Auto-generated method stub
	}

	private void setSpace(final ISpace space) {
		if (this.space != null) {
			if (this.space == space)
				return;
			this.space.removeChangeListener(spaceChangeListener);
			loadJob.cancel();
			Thread.yield();
		}

		initializeFlatWorldLayers();

		this.space = space;
		
		if (space != null) {
			setPartName("World - "+space.getName());//FIXME this is because the name changes and we dont get notified
			
			loadJob = new Job("World loading space '"+space.getName()+"'") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					Thread.yield();
					space.addChangeListener(spaceChangeListener);
					monitor.beginTask("Loading entities", space.size());
					for(IEntity entity: space) {
						addEntity(entity);
						monitor.worked(1);
						if (monitor.isCanceled()) {
							return Status.CANCEL_STATUS;
						}
					}
					
					updater.redraw();
					return Status.OK_STATUS;
				}
			};
			loadJob.setPriority(Job.BUILD);
			loadJob.schedule();
		} else {
			setPartName("World");
		}
		
		updater.redraw();
	}
	
	private void handleSpaceChange(final ISpaceContentChangeEvent event) {
		if (!(event.getEntity() instanceof AbstractEntity))
			return;
		final AbstractEntity entity = (AbstractEntity)event.getEntity();
		if(event.isEntityAddEvent()) {
			addEntity(entity);
		} else if(event.isEntityUpdateEvent()) {
			updateEntity(entity);
		} else if(event.isEntityRemoveEvent()) {
			removeEntity(entity);
		}
	}

	private synchronized void addEntity(IEntity entity) {
		if (entity.getTypeName().equals("host")) {
			addNode(entity);
			if (followNewEntities) {
				focusEntity(entity);
			}
		}
		
		for (ISemanticLayer layer: layers) {
			if (layer instanceof IEdgeLayer) {
				IEdgeLayer edgeLayer = (IEdgeLayer)layer;
				for (IEdge edge: edgeLayer.getEdges(entity)) {
					addEdge(edge);
				}
			}
		}
	}

	private synchronized void updateEntity(IEntity entity) {
		// TODO
		for (ISemanticLayer layer: layers) {
			if (layer instanceof IEdgeLayer) {
				IEdgeLayer edgeLayer = (IEdgeLayer)layer;
				for (IEdge edge: edgeLayer.getEdges(entity)) {
					addEdge(edge);
				}
			}
		}

		if (entity == focusEntity)
			focusEntity(entity); // refocus, update label
	}
	
	private synchronized void removeEntity(IEntity entity) {
	}

	private void addNode(IEntity entity) {
		final ILocation location = getLocation(entity);
		if (location != null) {
			final String label = location.getCity() != null ? location.getCity() : location.getCountry();
			if (label != null) {
				spotsLayer.addSpot(location.getPosition()[0], location.getPosition()[1], label, entity);
//				world.addRaindrop(location.getPosition()[0], location.getPosition()[1], red);
				updater.redraw();
			}
		}
	}

	private void addEdge(IEdge edge) {
/*		ILocation sourceLocation = getLocation(edge.getSource());
		ILocation targetLocation = getLocation(edge.getTarget());
		if (sourceLocation != null && targetLocation != null)
			world.addLine(sourceLocation.getPosition()[0], sourceLocation.getPosition()[1], targetLocation.getPosition()[0], targetLocation.getPosition()[1]);
*/	}

	private ILocation getLocation(IEntity entity) {
		for (ISemanticLayer layer: layers) {
			if (layer instanceof IGeographicalLayer) {
				ILocation location = ((IGeographicalLayer)layer).getLocation(entity);
				if (location != null) {
					return location;
				}
			}
		}
		
		return null;
	}

	public synchronized void focusEntity(IEntity entity) {
		final ILocation location = getLocation(entity);
		if (location != null) {
			focusLayer.setFocus(location.getPosition()[0], location.getPosition()[1]);
		} else {
			focusLayer.unsetFocus();
		}
		updater.redraw();
	}
	
	private void initializeFlatWorldLayers() {
		spotsLayer = new SpotsFlatWorldLayer<IEntity>();
//		raindropsLayer = new RaindropFlatWorldLayer();
		focusLayer = new FocusFlatWorldLayer();
		world.reset();
		world.addLayer(spotsLayer);
	}
}
