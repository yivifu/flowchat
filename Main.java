package flowchat;

import java.io.BufferedInputStream;
import java.io.FileInputStream;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class Main {
	private ASTParser astParser = ASTParser.newParser(AST.JLS3);
	private byte[] input;
	private char[] source;
	private final String SOURCE_PATH = "e:\\tmp\\";

	public CompilationUnit getCompilationUnit(String fileName) throws Exception {
		String pathname = String.format("%s%s", SOURCE_PATH, fileName);
		BufferedInputStream bufferedInputStream = new 
				BufferedInputStream(new FileInputStream(pathname));
		input = new byte[bufferedInputStream.available()];
		bufferedInputStream.read(input);
		bufferedInputStream.close();
		source = new String(input).toCharArray();
		this.astParser.setSource(source);
		CompilationUnit result = (CompilationUnit) (this.astParser.createAST(null));// 获取根节点
		return result;

	}

	public static void main(String[] args) {
		Main m = new Main();
		try {
			ASTNode root = m.getCompilationUnit("AssignmentStatementParser.java");
			boolean makedot = true;
			DotASTVistor visitor = new DotASTVistor(m.source, makedot );
			root.accept(visitor);
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
