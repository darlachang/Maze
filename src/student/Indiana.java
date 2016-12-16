package student;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import game.ScramState;
import game.HuntState;
import game.Explorer;
import game.Node;
import game.NodeStatus;

public class Indiana extends Explorer {

	private long startTime= 0;    // start time in milliseconds
	private HashSet<Long> visited = new HashSet<>();
	private boolean found = false; 
	private Heap<NodeStatus> unvisit = new Heap<NodeStatus>();
	private HashMap<Node, Integer> shortestDistanceToTheExist = new HashMap<Node, Integer>();
	private HashMap<Node, Integer> shortestDistanceFromTheCurrent = new HashMap<Node, Integer>();

	/** Get to the orb in as few steps as possible. Once you get there, 
	 * you must return from the function in order to pick
	 * it up. If you continue to move after finding the orb rather 
	 * than returning, it will not count.
	 * If you return from this function while not standing on top of the orb, 
	 * it will count as a failure.
	 * 
	 * There is no limit to how many steps you can take, but you will receive
	 * a score bonus multiplier for finding the orb in fewer steps.
	 * 
	 * At every step, you know only your current tile's ID and the ID of all 
	 * open neighbor tiles, as well as the distance to the orb at each of these tiles
	 * (ignoring walls and obstacles). 
	 * 
	 * In order to get information about the current state, use functions
	 * currentLocation(), neighbors(), and distanceToOrb() in HuntState.
	 * You know you are standing on the orb when distanceToOrb() is 0.
	 * 
	 * Use function moveTo(long id) in HuntState to move to a neighboring 
	 * tile by its ID. Doing this will change state to reflect your new position.
	 * 
	 * A suggested first implementation that will always find the orb, but likely won't
	 * receive a large bonus multiplier, is a depth-first search. Some
	 * modification is necessary to make the search better, in general.*/
	@Override public void huntOrb(HuntState state) {
		//TODO 1: Get the orb
		if(state.distanceToOrb() == 0){
			found = true;
			return;
		};
		ArrayList<NodeStatus> neighbors = new ArrayList<NodeStatus>(state.neighbors());	
		Collections.sort(neighbors);
		if(!visited.contains(state.currentLocation()))
			visited.add(state.currentLocation());
		for(int i = 0; i < neighbors.size(); i++){
			long goToNeighbor = neighbors.get(i).getId();
			if(!visited.contains(goToNeighbor)){
				long parent = state.currentLocation();
				state.moveTo(goToNeighbor);
				huntOrb(state); //backtrack
				if(found) return;
				state.moveTo(parent);
			}
		}
	}

	/** 
	 * Get out the cavern before the ceiling collapses, trying to collect as much
	 * gold as possible along the way. Your solution must ALWAYS get out before time runs
	 * out, and this should be prioritized above collecting gold.
	 * 
	 * You now have access to the entire underlying graph, which can be accessed through ScramState.
	 * currentNode() and getExit() will return Node objects of interest, and getNodes()
	 * will return a collection of all nodes on the graph. 
	 * 
	 * Note that the cavern will collapse in the number of steps given by getStepsRemaining(),
	 * and for each step this number is decremented by the weight of the edge taken. You can use
	 * getStepsRemaining() to get the time still remaining, pickUpGold() to pick up any gold
	 * on your current tile (this will fail if no such gold exists), and moveTo() to move
	 * to a destination node adjacent to your current node.
	 * 
	 * You must return from this function while standing at the exit. Failing to do so before time
	 * runs out or returning from the wrong location will be considered a failed run.
	 * 
	 * You will always have enough time to escape using the shortest path from the starting
	 * position to the exit, although this will not collect much gold. For this reason, using 
	 * Dijkstra's to plot the shortest path to the exit is a good starting solution    */
	@Override public void scram(ScramState state) {
		//TODO 2: Get out of the cavern before it collapses, picking up gold along the way

		//2 maps to record each node's shortest distance to the exist and distance to the current
		shortestDistanceToTheExitAndCurrent(state);
		List<Node> path = takeLargestGolds(state); //change the path method to here
		traverse(state, path);
	}
	//basic (Dijkstra)
	private List<Node> takeShortestPath(ScramState state){
		List<Node> sPath = Paths.shortestPath(state.currentNode(), state.getExit());
		return sPath;
	}
	//method 1
	private List<Node> takeOnlyLargestGold(ScramState state){
		//if the distance of largest gold to the exist < stepleft => go take that largest gold
		List<Node> path = new LinkedList<Node>();
		helper1(state, path);
		return path;
	}
	private void helper1(ScramState state, List<Node> path){
		Node gold = goldsOrder(state).poll(); //consider the largest gold
		System.out.println("gold ID " + gold.getId() + ", amount " + gold.getTile().gold());
		if(shortestDistanceFromTheCurrent.get(gold) + shortestDistanceToTheExist.get(gold) < state.stepsLeft()){
			//			List<Node> pathToCurrent = Paths.shortestPath(n, state.currentNode());
			//			int shortesetdistanceToCurrent = Paths.pathDistance(pathToCurrent);
		}
		Node gold1 = goldsOrder(state).poll();
		List<Node> path1 = Paths.shortestPath(state.currentNode(), gold);
		path.addAll(path1);

		/*we can't directly connect two paths : state-gold & gold-exit, 
		 * cuz gold couldn't moveTo gold. 
		 * So we have to start our second path from one of gold's neighbor 
		 * instead of gold itself.
		 */
		Set<Node> neighbors = gold.getNeighbors();
		for(Node n : neighbors){
			gold = n;
			break;
		}
		List<Node> path2 = Paths.shortestPath(gold, state.getExit());
		path.addAll(path2);
	}
	//method 2
	private List<Node> takeLargestGolds(ScramState state){
		List<Node> path = new LinkedList<Node>();
		helper2(state, path);
		return path;
	}
	private void helper2(ScramState state, List<Node> path){
		Heap<Node> gO = goldsOrder(state);
		boolean stillHaveSpace = true;
		ArrayList<Node> attemptedGolds = new ArrayList<Node>();
		ArrayList<Node> settledGolds = new ArrayList<Node>();
		List<Node> settledPath = new LinkedList<Node>();
		while(stillHaveSpace || settledGolds.size() == 0){
				Node gold = gO.poll();
				//build candidate golds' shortest path
				attemptedGolds.addAll(settledGolds);
				attemptedGolds.add(gold);
				ArrayList<Node> fixNodes = new ArrayList<Node>();
				fixNodes.add(state.currentNode());
				while(attemptedGolds.size() != 0){		
					//find the Node which has min distance to connectedPoint, add it.
					Node connectedPoint = fixNodes.get(fixNodes.size()-1);
					Node closestNode = attemptedGolds.get(0); //just for creating a min-node variable
					List<Node> shortestPath = Paths.shortestPath(connectedPoint, attemptedGolds.get(0)); //just for calculating min distance
					int shortestDistance = Paths.pathDistance(shortestPath); //min distance
					for(int i = 1 ; i < attemptedGolds.size(); i++){
						List<Node> goldPath = Paths.shortestPath(connectedPoint, attemptedGolds.get(i)); 
						int goldDistance = Paths.pathDistance(goldPath);
						if(goldDistance < shortestDistance){
							closestNode = attemptedGolds.get(i);
							shortestDistance = goldDistance;
						}
					}
					attemptedGolds.remove(closestNode);
					fixNodes.add(closestNode);
				}
				//fixNodes + exit -> nodes for attemptedPath
				fixNodes.add(state.getExit());
				List<Node> attemptedPath = new LinkedList<Node>();	
				attemptedPath.addAll(Paths.shortestPath(fixNodes.get(0), fixNodes.get(1)));
				for(int i = 1; i < fixNodes.size()-1;i++){
					Node neighbor = fixNodes.get(i);
					Set<Node> neighbors = neighbor.getNeighbors();
					for(Node n : neighbors){
						neighbor = n;
						break;
					}
					attemptedPath.addAll(Paths.shortestPath(neighbor, fixNodes.get(i+1)));
				}		
				int pathLength = Paths.pathDistance(attemptedPath);
				if(pathLength <= state.stepsLeft()){
					settledGolds.add(gold);
					settledPath = attemptedPath;
				}
				else{
					stillHaveSpace = false;
				}
		}
		path.addAll(settledPath);
	}
	
	private void shortestDistanceToTheExitAndCurrent(ScramState state){
		/*map is used to record every node's shortest distance to the exit */
		//HashMap<Node, Integer> pureShortestDistanceToExit = new HashMap<Node, Integer>();
		for(Node n: state.allNodes()){
			List<Node> pathToExit = Paths.shortestPath(n, state.getExit());
			int shortesetdistanceToExist = Paths.pathDistance(pathToExit);
			shortestDistanceToTheExist.put(n, shortesetdistanceToExist);

			List<Node> pathToCurrent = Paths.shortestPath(n, state.currentNode());
			int shortesetdistanceToCurrent = Paths.pathDistance(pathToCurrent);
			shortestDistanceFromTheCurrent.put(n, shortesetdistanceToCurrent);
		}
	}
	private Heap<Node> goldsOrder(ScramState state){
		Heap<Node> goldsOrder = new Heap<Node>();
		for(Node n : state.allNodes()){
			goldsOrder.add(n, -n.getTile().gold());
		}
		return goldsOrder;
	}

	/** Indiana is standing on the first node of path p.
	 * Traverse the path, picking up any gold on it. */
	public void traverse(ScramState state, List<Node> path) {
		int k= 0; // the number of nodes processed
		for (Node n: path) {
			// pick up gold in current state
			if(state.currentNode().getTile().gold() != 0)
				state.grabGold();
			if(k!=0){
				state.moveTo(n);
			}
			k++;
		}
	}
}
