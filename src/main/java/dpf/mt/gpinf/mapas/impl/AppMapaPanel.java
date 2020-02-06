package dpf.mt.gpinf.mapas.impl;

import java.awt.BorderLayout;
import java.awt.Component;
import java.io.IOException;
import java.util.HashMap;

import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import dpf.mt.gpinf.indexer.search.kml.KMLResult;
import dpf.mt.gpinf.mapas.AbstractMapaCanvas;
import dpf.mt.gpinf.mapas.webkit.MapaCanvasWebkit;
import dpf.sp.gpinf.indexer.search.ItemId;
import gpinf.dev.data.EvidenceFile;
import dpf.sp.gpinf.indexer.desktop.App;
import dpf.sp.gpinf.indexer.desktop.MapaModelUpdateListener;
import dpf.sp.gpinf.indexer.parsers.util.ExtraProperties;

/* 
 * Classe que controla a integração da classe App com a classe MapaCanvas
 */

public class AppMapaPanel extends JPanel {
	AbstractMapaCanvas browserCanvas;
	final App app;
    boolean mapaDesatualizado = true; //variável para registrar se os dados a serem apresentados pelo mapa precisa renderização 
    KMLResult kmlResult;
	
	public AppMapaPanel(App app){
		this.app = app;
	    this.setLayout(new BorderLayout());
	    
	    init();
	}
	
	public void init(){
	    browserCanvas = new MapaCanvasWebkit();
	    browserCanvas.addSaveKmlFunction(new Runnable() {
			public void run() {
				KMLResult kml = new KMLResult();
				kml.saveKML();
			}
		});
	    browserCanvas.setMapSelectionListener(new AppMapaSelectionListener());
	    browserCanvas.setMarkerEventListener(new AppMapMarkerEventListener());
	    browserCanvas.setMarkerCheckBoxListener(new AppMarkerCheckBoxListener());

	    //Adiciona listener para indicar a seleção de item ao Mapa
	    app.getResultsTable().getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
			    if(e.getValueIsAdjusting()) return;

				if((!mapaDesatualizado)){
					ListSelectionModel lsm = (ListSelectionModel) e.getSource();
					HashMap<String, Boolean> selecoes = new HashMap<String, Boolean>(); 
					for(int i = e.getFirstIndex(); i <= e.getLastIndex(); i++){
						boolean selected = lsm.isSelectedIndex(i);

						int rowModel = app.getResultsTable().convertRowIndexToModel(i);
						ItemId item = app.getResults().getItem(rowModel);
						
						if(kmlResult != null && kmlResult.getGPSItems().contains(item)) {
						    String gid = "marker_" + item.getSourceId() + "_" + item.getId(); //$NON-NLS-1$ //$NON-NLS-2$
                            selecoes.put(gid, selected);
						}
					}
					browserCanvas.enviaSelecoes(selecoes);
					
					if(App.get().mapTabDock.isShowing()){
						browserCanvas.redesenha();
					}
				}
			}
		});

	    app.resultsModel.addTableModelListener(new MapaModelUpdateListener(app));

	    this.add(browserCanvas.getContainer(), BorderLayout.CENTER);
	}

	public void redesenhaMapa(){
		    if(mapaDesatualizado && (app.getResults().getLength()>0)){
		    	//se todo o modelo estiver desatualizado, gera novo KML e recarrega todo o mapa
				if(!browserCanvas.isConnected()){
					this.setVisible(true);

					browserCanvas.connect();

					//força a rederização do Mapa (resolvendo o bug da primeira renderização 
					for (Component c : app.mapTabDock.getContentPane().getComponents()) {
						c.repaint();
					}
					app.mapTabDock.getContentPane().repaint();
				}

			    String kml = ""; //$NON-NLS-1$
			    try {
			        kmlResult = new KMLResult();
			    	kml = kmlResult.getResultsKML();
			    	browserCanvas.setKML(kml);
				} catch (IOException e1) {
					e1.printStackTrace();
				}finally {
					mapaDesatualizado = false;
				}
			}else{
				browserCanvas.redesenha();
			}
	  }
	
	public void redesenha() {
	    browserCanvas.redesenha();
	    mapaDesatualizado = false;
	}

	public boolean isMapaDesatualizado() {
		return mapaDesatualizado;
	}

	public void setMapaDesatualizado(boolean mapaDesatualizado) {
		this.mapaDesatualizado = mapaDesatualizado;
	}
	
	public void selecionaMarcador(ItemId item, boolean b){
	    
	    if(kmlResult != null && kmlResult.getGPSItems().contains(item)) {
	        String gid = "marker_" + item.getSourceId() + "_" + item.getId(); //$NON-NLS-1$ //$NON-NLS-2$
	        browserCanvas.selecionaMarcador(gid, b);
	    }
	}

}