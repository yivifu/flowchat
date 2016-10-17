package flowchat;
/**
 * 流程图dot节点工厂类，不同类型的节点主要体现在形状不同
 * @author 华硕
 *
 */
public class DotNodeFac {
	/**
	 * 创建无操作节点，用于将if、switch等可能有多个分支的语句
	 * 的分支出口指向同一个节点，即无操作节点
	 * @return
	 */
	public static DotNode createNoOpNode() {
		DotNode rtNode = new DotNode();
		rtNode.setShape("circle"); 
		return rtNode;
		
	}
	/**
	 * 创建条件判断类节点
	 * @return
	 */
	public static DotNode createDiamondNode() {
		DotNode rtNode = new DotNode();
		rtNode.setShape("diamond"); 
		return rtNode;
		
	}
	/**
	 * 创建普通节点
	 * @return
	 */
	public static DotNode createRecordNode() {
		DotNode rtNode = new DotNode();
		return rtNode;
	}
	/**
	 * 创建结束节点
	 * @return
	 */
	public static DotNode createEndNode() {
		DotNode rtNode = new DotNode();
		rtNode.setShape("doublecircle"); 
		rtNode.setText("end");
		return rtNode;
	}
}
