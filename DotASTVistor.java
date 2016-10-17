package flowchat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BlockComment;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.LineComment;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
/**
 * Eclipse AST树访问者类
 * @author 华硕
 *
 */


public class DotASTVistor extends ASTVisitor {
	public List<DotNode> nodeList;
	private int nodeNum;
	private char[] input;
	public final String DOT_PATH = "e:\\tmp\\";
	public List<String> dotFiles = new ArrayList<>();
	private boolean makedot = true;
	
	public DotASTVistor(char[] input, boolean makedot){
		this.input = input;
		this.makedot = makedot;
	}
	
	public boolean visit(MethodDeclaration node){
		Block block = node.getBody();
		nodeList = new ArrayList<>();
		nodeNum = 0;
		try {
			makeList(block, nodeList);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		String methodName = node.getName().toString();
		if(nodeList.size() > 0){
			//DotNode endNode = DotNodeFac.createEndNode();
			//endNode.setId(++nodeNum);
			//endNode.addPreId(nodeList, getMaxId(nodeList));
			//nodeList.add(endNode);//添加结束节点
			for(DotNode dotNode : nodeList){//将制作流程链表时临时生成的控制语句公共出口节点过滤掉
				if(dotNode.getText() == ""){
					for(DotNode dNode : nodeList){
						if(dNode.getPreIds().contains(dotNode.getId())){
							dNode.getPreIds().remove((Integer)dotNode.getId());
							dNode.getPreIds().addAll(dotNode.getPreIds());
							dotNode.getPreIds().clear();
							Set<Integer> keys  = dotNode.getEdgeLbls().keySet();
							for(int key : keys){//空节点边的关系转向其后续结点
								dNode.setEdgeLbl(key, dotNode.getEdgeLbl(key));
							}
						}
					}
				} else	if(dotNode.getText().startsWith("return ") || 
						dotNode.getText().startsWith("return;")){					
					for(DotNode dNode : nodeList){						
						if(dNode.getPreIds().contains(dotNode.getId())){
								dNode.getPreIds().remove((Integer)dotNode.getId());
						}
					}
					
					//endNode.addPreId(dotNode.getId());//所有返回语句都指向end节点	
				}		
			}
		}
		
		String pathname = String.format("%s%s", DOT_PATH, methodName);
		if(dotFiles.contains(pathname)){
			pathname += "_1";//有重载的方法则修改文件名以免覆盖
		}
		dotFiles.add(pathname);
		try {
			if(makedot){
				makeDotFile(pathname);//生成dot文件
			}
			makePng(pathname);//生成png格式的流程图
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}
	/**
	 * 生成一个java编译单元中所有方法的dot流程描述文件
	 * @param pathname
	 * @throws FileNotFoundException
	 */
	private void makeDotFile(String pathname) throws FileNotFoundException {
		PrintStream ps = new PrintStream(new File(pathname + ".txt"));
		ps.println(String.format("%s%s%s%s%s%s%s", //开始节点用圆形，图形标题带方法名
				"digraph G{\n\tgraph [fontname=\"Microsoft YaHei\"];\n\t",
				"subgraph cluster_g{\n\t\tlabel=<<font color=\"red\">",
				pathname.substring(pathname.lastIndexOf('\\') + 1),"流程图",
				 "</font>>;\n\t\tnode [shape=record,fontname=\"Microsoft YaHei\"];\n",
				"\t\tedge[fontname=\"Microsoft YaHei\"];\n\n",
				"\t\tnode0[shape=circle,label=\"start\",style=\"filled\",fillcolor=green];"));
		
		for(DotNode n : this.nodeList){
			if(n.getText() != ""){//过滤掉制作执行链表时用于过渡的空节点
				ps.println(String.format("\t\tnode%d[label=\"%d:\\n%s\",shape=%s];",
						n.getId(), n.getId(),n.getText(),n.getShape()));
				for(int i : n.getPreIds()){
					String label = n.getEdgeLbl(i) == "" ? 
							"" : String.format("[label=\"%s\"]",  n.getEdgeLbl(i));
					ps.println(String.format("\t\tnode%d->node%d%s;", i, n.getId(), label));
				}
			}
		}
		ps.println("\t}\n}");//结束dot文件
	}
	/**
	 * 执行dot命令行命令，从dot脚本生成png流程图
	 * @param pathname
	 * @throws Exception
	 */
	private void makePng(String pathname)throws Exception {
		String exec = String.format("dot  -Tpng %s.txt -o %s.png", pathname, pathname);
		Runtime.getRuntime().exec(exec);
		
	}
	/**
	 * 将源代码作为标签显示为流程图节点的内容
	 * 将源代码中可能出现的dot敏感字符替换（除了双引号和尖括号，不知道还有没有）
	 * @param node
	 * @return
	 * @throws UnsupportedEncodingException 
	 */
	private String getSource(ASTNode node)throws Exception{
		String source = new String(input, node.getStartPosition(), node.getLength());
		source = source.replace("\"", "\\\"");
		source = source.replace("<", "&lt;");
		source = source.replace(">", "&gt;");
		return source;
	}
	/**
	 * 获取dot结点链表中id最大的节点的id，主要便于if语句
	 * 判断节点为假时与正确的后续执行结点相连
	 * @param list
	 * @return
	 */
	private int getMaxId(List<DotNode> list){
		int maxId = 0;
		for(DotNode node : list){
			if(maxId < node.getId()){
				maxId = node.getId();
			}
		}
		return maxId;
	}
	
	/**
	 * 生成方法的执行流程链表
	 * @throws UnsupportedEncodingException 
	 */
	private List<DotNode> makeList(ASTNode node, List<DotNode> list) throws Exception{
		int nodeType = node.getNodeType();
		switch(nodeType){			
			case 8:{//block;大括号包围的语句块
				Block block = (Block)node;
				List<ASTNode> li = (List<ASTNode>)block.statements();
				//依次取出语句块中的各语句，递归调用流程链表生成方法，实现递归下降
				for(int i=0; i < li.size(); i++){
					ASTNode tmp = li.get(i);
					if(!(tmp instanceof LineComment || tmp instanceof BlockComment))
						DotNode.listAdd(list, makeList(tmp,list));
				}
				return list;
			}
			case 51:{//synchronized包含的语句，处理方法与block类似
				SynchronizedStatement synStmt = (SynchronizedStatement)node;
				Block body = synStmt.getBody();				
				if(body != null){//body本身是block，case 8已处理块里语句的遍历，这里无需再遍历
					DotNode.listAdd(list, makeList(body, list));
				}				
				return list;
			}
			case 54:{//try-catch语句，处理方法与synchronized类似，仅将try块中的语句内入流程
				TryStatement synStmt = (TryStatement)node;
				Block body = synStmt.getBody();				
				if(body != null){
					DotNode.listAdd(list, makeList(body, list));
				}				
				return list;
			}
			case 25:{//IF语句
				//为减轻链表合并负担，创建局部变量
				List<DotNode> l = new ArrayList<>();
				//条件节点
				DotNode dotNode = DotNodeFac.createDiamondNode();
				IfStatement ifStatement = (IfStatement)node;
				Expression expr = ifStatement.getExpression();
				dotNode.setText(getSource(expr));
				//注意将设置节点的文本放在添加节点的前导节点之前
				dotNode.addPreId(list, nodeNum);
				DotNode.listAdd(l, dotNode);
				dotNode.setId(++nodeNum);
				//由于递归过程中nodeNum会变化，用临时变量将条件节点的id记下来
				int p = nodeNum;//不过也可以用dotNode.getId代替临时变量
				//用于将if语句各分支出口汇合的空节点，链表制作完成后再过滤掉
				DotNode outNode = DotNodeFac.createNoOpNode();
				//条件成立时执行的语句
				Statement then = ((IfStatement)node).getThenStatement();
				ArrayList<Integer> ifst = new ArrayList<>();
				if(then != null){
					List<DotNode> lt = new ArrayList<>();					
					lt = makeList(then, lt);
					if(lt.size() > 0){
						DotNode.listAdd(l, lt);
						lt.get(0).getPreIds().clear();
						lt.get(0).setEdgeLbl(p, "yes");
						lt.get(0).addPreId(list, p);
						//if语句中id最大的是条件语句的id，但由于其创建时间较早，
						//在链表中的位置靠前，其出口应从条件语句id引出
						ifst.add(getMaxId(lt));
						
					}
				}
				//条件不成立时执行的语句
				Statement elseStmt = ((IfStatement)node).getElseStatement();
				if(elseStmt != null){					
					List<DotNode> le = new ArrayList<>();
					le = makeList(elseStmt, le);
					if(le.size() > 0){
						DotNode.listAdd(l, le);
						le.get(0).getPreIds().clear();						
						le.get(0).setEdgeLbl(p, "no");
						le.get(0).addPreId(list, p);
						ifst.add(getMaxId(le));
						
					}
				} else {//没有else语句，则直接链接出口节点，并标明系条件为no的流程
					outNode.setEdgeLbl(p, "no");
					outNode.addPreId(list, p);
				}				
				outNode.setId(++nodeNum);
				DotNode.listAdd(l, outNode);
				DotNode.listAdd(list, l);
				if(ifst.size() > 0){
					for(int i : ifst){
						outNode.addPreId(list, i);
					}					
				}
				return list;
			}
			case 61:{//while语句
				List<DotNode> l = new ArrayList<>();
				DotNode dotNode = DotNodeFac.createDiamondNode();
				WhileStatement whileStmt = (WhileStatement)node;
				Expression expr = whileStmt.getExpression();
				dotNode.setText(getSource(expr));
				dotNode.addPreId(list, nodeNum);
				DotNode.listAdd(list, dotNode);
				Statement body = whileStmt.getBody();
				if(body != null){
					l = makeList(body, l);
				}
				DotNode.listAdd(list, l);
				dotNode.setId(++nodeNum);
				int p = nodeNum;
				if(l.size() > 0){
					l.get(0).getPreIds().clear();
					l.get(0).setEdgeLbl(p, "yes");
					l.get(0).addPreId(list, p);
					DotNode.getDotNode(list, p).addPreId(list, 
							l.get(l.size() - 1).getId());
				}				
				return list;
			}
			case 50:{//switch语句
				List<DotNode> l = new ArrayList<>();
				DotNode dotNode = DotNodeFac.createDiamondNode();
				SwitchStatement switchStmt = (SwitchStatement)node;
				Expression expression = switchStmt.getExpression();
				dotNode.setText(getSource(expression));
				dotNode.addPreId(list, nodeNum);
				DotNode.listAdd(l, dotNode);
				dotNode.setId(++nodeNum);
				int p = nodeNum;
				DotNode outNode = DotNodeFac.createNoOpNode();
				List<ASTNode> li = switchStmt.statements();
				List<Integer> caseList = new ArrayList<>();
				for(int i=0; i < li.size(); i++){
					
					ASTNode tmp = li.get(i);
					//所有的case都从switch标志语句引出
					if(tmp instanceof SwitchCase){
						dotNode = DotNodeFac.createRecordNode();
						dotNode.setId(++nodeNum);
						dotNode.setText(getSource(tmp).replace("case", ""));
						dotNode.addPreId(list, p);
						dotNode.setEdgeLbl(p, "case");
						DotNode.listAdd(l, dotNode);
						caseList.add(nodeNum);
					}else{	
						//case分支的语句块汇合接指向空的出口节点
						List<DotNode> tmpList = new ArrayList<>();
						tmpList = (ArrayList<DotNode>) makeList(tmp, tmpList);
						if(tmpList.size() >  0){
							outNode.addPreId(tmpList.get(tmpList.size() - 1).getId());
							if(tmp instanceof BreakStatement || tmp instanceof ReturnStatement){
								outNode.getPreIds().removeAll(tmpList.get(tmpList.size() - 1).getPreIds());
							}
							for(int j = 0; j < caseList.size(); j++){
								tmpList.get(0).addPreId(caseList.get(j));
							}
							caseList.clear();
							DotNode.listAdd(l, tmpList);
							for(DotNode n1 : l){
								for(DotNode n2 : l){
									if(n2.getPreIds().contains(n1.getId())){
										if(n1.getShape().equals("diamond") && 
												n2.getEdgeLbl(n1.getId()) == ""){
											n2.setEdgeLbl(n1.getId(), "no");
											
										}
									}
								}
							}
						}						
					}
				}
				if(l.size() > 1){
					outNode.setId(++ nodeNum);
					DotNode.listAdd(l, outNode);
				}else{//万一碰上空的switch语句，直接从switch标志节点连上出口节点
					outNode.addPreId(p);
				}
				DotNode.listAdd(list, l);
				return list;
			}
			case 24:{// for
				List<DotNode> l = new ArrayList<>();
				DotNode dotNode = DotNodeFac.createDiamondNode();
				ForStatement forStmt = (ForStatement)node;
				Expression expression = forStmt.getExpression();
				dotNode.setText(getSource(expression));
				dotNode.addPreId(list, nodeNum);
				DotNode.listAdd(list, dotNode);
				
				Statement body = forStmt.getBody();
				if(body != null){
					l = makeList(body, l);
				}
				dotNode.setId(++nodeNum);
				int p = nodeNum;
				if(l.size() > 0){
					l.get(0).getPreIds().clear(); 
					l.get(0).setEdgeLbl(p, "yes");
					l.get(0).addPreId(list, p);
					DotNode.getDotNode(list, p).addPreId(l.get(l.size() - 1).getId());
				}
				DotNode.listAdd(list, l);
				return list;
			}
			case 19:{//do-while
				List<DotNode> l = new ArrayList<>();
				DotNode dotNode = DotNodeFac.createDiamondNode();
				DoStatement doStmt = (DoStatement)node;
				Expression expr = doStmt.getExpression();
				dotNode.setText(getSource(expr));
				DotNode.listAdd(list, dotNode);
				Statement body = doStmt.getBody();
				if(body != null){
					l = makeList(body, l);
				}
				//因为do-while先执行语句，所以要先将链表合并在添加前导节点
				DotNode.listAdd(list, l);
				dotNode.addPreId(list, nodeNum);
				dotNode.setId(++nodeNum);
				if(l.size() > 0){
					l.get(0).setEdgeLbl(dotNode.getId(), "yes");
					l.get(0).addPreId(list, dotNode.getId());
				}
				return list;
			}
			
			case 70:{//for-each形式的语句
				EnhancedForStatement forEachStmt = (EnhancedForStatement)node;
				Expression expression = forEachStmt.getExpression();
				String expr = expression.toString();
				String param = forEachStmt.getParameter().getName().toString();
				List<DotNode> l = new ArrayList<>();
				DotNode dotNode = DotNodeFac.createDiamondNode();							
				dotNode.setText(String.format("%s:%s", param, expr));
				dotNode.addPreId(list, nodeNum);
				DotNode.listAdd(list, dotNode);
				
				Statement body = forEachStmt.getBody();
				if(body != null){
					l = makeList(body, l);
				}
				dotNode.setId(++nodeNum);
				int p = nodeNum;
				if(l.size() > 0){
					l.get(0).getPreIds().clear();
					l.get(0).setEdgeLbl(p, "yes");
					l.get(0).addPreId(list, p);
					DotNode.getDotNode(list, p).addPreId(l.get(l.size() - 1).getId());
				}
				DotNode.listAdd(list, l);
				return list;
			}
			
			case 60:{//variable declare，减小节点内容长度，删除声明中的类型
				DotNode dotNode = DotNodeFac.createRecordNode();
				VariableDeclarationStatement varStmt = (VariableDeclarationStatement)node;
				String type = varStmt.getType().toString() + " ";
				dotNode.setText(getSource(varStmt).replace(type, ""));
				dotNode.addPreId(list, nodeNum);
				DotNode.listAdd(list, dotNode);
				dotNode.setId(++nodeNum);
				return list;
			}
			case 18://continue
			case 41://return			
			case 21://expression
			case 10:{//break，这几种都是简单的原子语句，直接加入流程
				DotNode dotNode = DotNodeFac.createRecordNode();
				dotNode.setText(getSource(node));
				dotNode.addPreId(list, nodeNum);
				DotNode.listAdd(list, dotNode);
				dotNode.setId(++nodeNum);
				return list;
			}
			
			default:
				return list;
			
		}
	}	
}
