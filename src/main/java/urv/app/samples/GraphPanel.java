package urv.app.samples;

import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.VertexPaintFunction;
import edu.uci.ics.jung.graph.decorators.VertexStringer;
import edu.uci.ics.jung.graph.impl.DirectedSparseGraph;
import edu.uci.ics.jung.io.PajekNetReader;
import edu.uci.ics.jung.utils.Pair;
import edu.uci.ics.jung.visualization.*;
import edu.uci.ics.jung.visualization.SpringLayout;
import edu.uci.ics.jung.visualization.contrib.CircleLayout;
import edu.uci.ics.jung.visualization.contrib.KKLayout;
import edu.uci.ics.jung.visualization.contrib.KKLayoutInt;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.control.ScalingControl;
import urv.machannel.MChannel;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

/*
 * This class extends from jPanel, and it's a panel that contains a graph
 */
public class GraphPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	private JComboBox jComboBoxLayout = null;
	private JButton jButtonZoomPlus = null;
	private JButton jButtonZoomMinus = null;
	private GraphZoomScrollPane scrollGraphPanel = null;
	
	private ScalingControl scaler = null;
	private Graph netGraph = null;  //  @jve:decl-index=0:
	private VisualizationViewer vv = null;
	private Layout l = null;
	
	private final MChannel groupChannel;
	private JPanel jPanel1;

	/**
	 * This is the default constructor
	 */
	public GraphPanel(MChannel groupChannel) {
		super();		
		this.groupChannel = groupChannel;		
		initialize();
	}

	private static String shortIPName(String ip){
		int index1stDot = ip.indexOf('.', 0);
		if (index1stDot==-1){
			return null;
		}
		int index2ndDot = ip.indexOf('.',index1stDot+1);
		if (index2ndDot==-1){
			return null;
		}
		int indexColon = ip.indexOf(':');
		if (indexColon==-1){
			return null;
		}
		return ip.substring(index2ndDot+1, indexColon);
	}	
	/*
	 * Method that read a pajek network file and create the graph
	 */
	public void loadGraphFromPajekFile(String fileName){		
		Graph g = null;	
		PajekNetReader pajekReader = new PajekNetReader();
		try {
			g = pajekReader.load(fileName);
			if(compareGraph(g)){//The graph is load in the layout, only if it is diferent
				netGraph = pajekReader.load(fileName);
				changeLayout();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 * Method that gets the selected Layout and load it in the graph
	 */
	private void changeLayout(){
		String selection = (String)jComboBoxLayout.getSelectedItem();

		switch(selection) {
			case "StaticLayout":
				l=new StaticLayout(netGraph);
				break;
			case "SpringLayout":
				l=new SpringLayout(netGraph);
				break;
			case "ISOMLayout":
				l=new ISOMLayout(netGraph);
				break;
			case "KKLayoutInt":
				l=new KKLayoutInt(netGraph);
				break;
			case "KKLayout":
				l=new KKLayout(netGraph);
				break;
			case "FRLayout":
				l=new FRLayout(netGraph);
				break;
			case "CircleLayout":
				l=new CircleLayout(netGraph);
				break;
		}
		vv.setGraphLayout(l);
	}

	/*
	 * Method that compares the two graph
	 */	
	private boolean compareGraph(Graph g){
		
		//if the number of vertices is different, the graph changes
		if(g.numVertices()!=netGraph.numVertices())
			return true;
		//if the number of edges is different, the graph changes
		if(g.numEdges()!=netGraph.numEdges())
			return true;		
		boolean found=false;
		Set setVertex = netGraph.getVertices();
		Set setVertexTmp = g.getVertices();
		Vertex v;
		Vertex vTmp;
		//For all the vertex of both graphs, search if all the names of 
		//the nodes are the same, if it has changed, the graph is different
		for (Iterator iter = setVertex.iterator(); iter.hasNext(); ){
			v = (Vertex)iter.next();
			found=false;
			for (Iterator iterTmp = setVertexTmp.iterator(); iterTmp.hasNext(); ){
				vTmp = (Vertex)iterTmp.next();
				if(v.getUserDatum(PajekNetReader.LABEL).equals(vTmp.getUserDatum(PajekNetReader.LABEL))){
					found=true;
					break;
				}
			}
			if(found==false)//this name is left
				return true;
		}		
		Set setEdges = netGraph.getEdges();
		Set setEdgesTmp = g.getEdges();
		Edge e;
		Edge eTmp;
		//For all the edges, search if they have the same ends.
		for (Iterator iter = setEdges.iterator(); iter.hasNext(); ){
			e = (Edge)iter.next();
			found=false;
			for (Iterator iterTmp = setEdgesTmp.iterator(); iterTmp.hasNext(); ){
				eTmp = (Edge)iterTmp.next();
				if(comparePairVertices(e.getEndpoints(),eTmp.getEndpoints())){
					found=true;
					break;
				}
			}
			if(found==false)
				return true;
		}		
		return false;
	}

	/*
	 * Method that helps to comprar a pair of nodes, the ends of a edge, and comprar if they are the same nodes
	 * 
	 * EX: (n1,n2) == (n3,n4) if n1=n3 && n2=n4 || n1=n4 && n2=n3
	 *  
	 */
	private static boolean comparePairVertices(Pair p, Pair pTmp) {
		return ((Vertex)p.getFirst()).getUserDatum(PajekNetReader.LABEL).equals(((Vertex)pTmp.getFirst()).getUserDatum(PajekNetReader.LABEL)) && ((Vertex)p.getSecond()).getUserDatum(PajekNetReader.LABEL).equals(((Vertex)pTmp.getSecond()).getUserDatum(PajekNetReader.LABEL)) || ((Vertex)p.getFirst()).getUserDatum(PajekNetReader.LABEL).equals(((Vertex)pTmp.getSecond()).getUserDatum(PajekNetReader.LABEL)) && ((Vertex)p.getSecond()).getUserDatum(PajekNetReader.LABEL).equals(((Vertex)pTmp.getFirst()).getUserDatum(PajekNetReader.LABEL));
	}
	/**
	 * This method initializes getJButtonZoomMinus	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getGetJButtonZoomMinus() {
		if (jButtonZoomMinus == null) {
			jButtonZoomMinus = new JButton();
			jButtonZoomMinus.setBounds(new Rectangle(243, 535, 41, 20));
			jButtonZoomMinus.setText("-");
			jButtonZoomMinus.addActionListener(e -> scaler.scale(vv, 1/1.1f, vv.getCenter()));
		}
		return jButtonZoomMinus;
	}	
	/**
	 * This method initializes getJButtonZoomPlus	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getGetJButtonZoomPlus() {
		if (jButtonZoomPlus == null) {
			jButtonZoomPlus = new JButton();
			jButtonZoomPlus.setBounds(new Rectangle(193, 535, 41, 20));
			jButtonZoomPlus.setText("+");
			jButtonZoomPlus.addActionListener(e -> scaler.scale(vv, 1.1f, vv.getCenter()));
		}
		return jButtonZoomPlus;
	}	
	/**
	 * This method initializes jComboBoxLayout	
	 * 	
	 * @return javax.swing.JComboBox	
	 */
	private JComboBox getJComboBoxLayout() {
		if (jComboBoxLayout == null) {
			jComboBoxLayout = new JComboBox(new String[]{"StaticLayout","SpringLayout","ISOMLayout",
					"KKLayoutInt","KKLayout","FRLayout","CircleLayout"});
			
			jComboBoxLayout.setSelectedIndex(4);

			jComboBoxLayout.setBounds(new Rectangle(5, 535, 172, 21));
			jComboBoxLayout.addItemListener(e -> changeLayout());
		}
		return jComboBoxLayout;
	}
	
	/**
	 * This method initializes scrollGraphPanel	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private GraphZoomScrollPane getScrollGraphPanel() {
		if (scrollGraphPanel == null) {
			scaler = new CrossoverScalingControl();
						
			iniGraph();
	        
	        scrollGraphPanel = new GraphZoomScrollPane(vv);
			scrollGraphPanel.setBounds(new Rectangle(0, 0, 400, 524));
			
		}
		return scrollGraphPanel;
	}
	/*
	 * Method that initializes the graph visualization 
	 */
	private void iniGraph(){
		
		netGraph = new DirectedSparseGraph();

		l = new KKLayout( netGraph );
		
		PluggableRenderer r = new PluggableRenderer();
		vv = new VisualizationViewer( l, r );		
		vv.setPreferredSize(new Dimension(400, 560));
		
		//Labels for the nodes
		VertexStringer vstringer =v -> {
            Object label = v.getUserDatum(PajekNetReader.LABEL);
            return (String)label;
        };
        r.setVertexStringer(vstringer);		
        //how to paint the nodes
		r.setVertexPaintFunction(new VertexPaintFunction(){
			public Paint getDrawPaint(Vertex v) {
				return Color.BLACK;
			}
			public Paint getFillPaint(Vertex v) {
				String label = (String)v.getUserDatum(PajekNetReader.LABEL);				
				String localIp = groupChannel.getLocalAddress().toString();
				String shortName = shortIPName(localIp);				
				//If it is me, 
				if (label.endsWith(shortName)){
					return Color.GREEN;
				} else {
					return Color.RED;
				}
			}			
		});        
	}
	
	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {

		java.awt.GridBagConstraints gridBagConstraints;

        jPanel1 = new javax.swing.JPanel();
        
        jComboBoxLayout = new JComboBox(new String[]{"StaticLayout","SpringLayout","ISOMLayout","KKLayoutInt","KKLayout","FRLayout","CircleLayout"});
		
        jButtonZoomPlus = new javax.swing.JButton();
        jButtonZoomMinus = new javax.swing.JButton();

        setLayout(new java.awt.BorderLayout());

        jPanel1.setLayout(new java.awt.GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        jPanel1.add(jComboBoxLayout, gridBagConstraints);
        
        jButtonZoomPlus.setText("+");
        jButtonZoomPlus.setPreferredSize(new Dimension(45,25));
        jPanel1.add(jButtonZoomPlus, new java.awt.GridBagConstraints());

        jButtonZoomMinus.setText("-");
        jButtonZoomMinus.setPreferredSize(new Dimension(45,25));
        jPanel1.add(jButtonZoomMinus, new java.awt.GridBagConstraints());
        
        if (scrollGraphPanel == null) {
			scaler = new CrossoverScalingControl();
			iniGraph();
	        scrollGraphPanel = new GraphZoomScrollPane(vv);
		}
        
        add(jPanel1, java.awt.BorderLayout.SOUTH);
        add(scrollGraphPanel,java.awt.BorderLayout.CENTER);
        
        jComboBoxLayout.setSelectedIndex(4);
		jComboBoxLayout.setBounds(new Rectangle(5, 535, 172, 21));
		
		/*============================ setup action listeners =================================*/
		
		jComboBoxLayout.addItemListener(e -> changeLayout());
		
		jButtonZoomPlus.addActionListener(e -> scaler.scale(vv, 1.1f, vv.getCenter()));
		jButtonZoomMinus.addActionListener(e -> scaler.scale(vv, 1/1.1f, vv.getCenter()));
	}
}