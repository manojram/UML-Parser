
import japa.parser.JavaParser;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.ConstructorDeclaration;
import japa.parser.ast.body.FieldDeclaration;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.Parameter;
import japa.parser.ast.body.TypeDeclaration;
import japa.parser.ast.stmt.Statement;
import japa.parser.ast.type.ClassOrInterfaceType;
import japa.parser.ast.visitor.VoidVisitorAdapter;
import net.sourceforge.plantuml.SourceStringReader;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class UMLParser {

	public static String sourceBuffer = "@startuml\n"; 
	public static String classIdentifier;
	
	public static  ArrayList<String> buffer = new ArrayList<String>();
	
	public static  ArrayList<String> interfacelist = new ArrayList<String>();
	public static  ArrayList<String> classlist = new ArrayList<String>();
	
	public static  ArrayList<ClassOrInterfaceType> implementslist = new ArrayList<ClassOrInterfaceType>();
	public static  ArrayList<ClassOrInterfaceType> extendslist = new ArrayList<ClassOrInterfaceType>();
	
	
	public static  ArrayList<String> privateatt = new ArrayList<String>();

	@SuppressWarnings("unchecked")
	
	public static void main(String[] args) throws FileNotFoundException,NullPointerException,Exception {

		int i=0,j=0,k=0,m=0;
		ArrayList<String> keywords = new ArrayList<String>();
		 ArrayList<String> line = new ArrayList<String>();
		
		/*File shall take the path from the command line argument with the first argument and shall save the png in the same folder*/
		 
		File root = new File(args[0]);

		File[] allfiles = root.listFiles();
		if (allfiles != null) {
			for (File child : allfiles) {
				
				/*in OS X. there is a ds-store file which will also loop without this check */
				
				if((child.getName().contains(".java"))) 
				{
					File file = new File(child.getAbsolutePath());

					FileInputStream in = new FileInputStream(file);
					CompilationUnit cu;
					try {

						cu = JavaParser.parse(in,"UTF-8");
					}
					
					finally {
						in.close();
					}


                    String array[] = cu.toString().split("\\r?\\n");
                                         
                     for(i=0;i<array.length;i++)
                     {
                   	  line.add(array[i]);
                   	 
                   	  
                   	  for(String l:line)
                   	  {
                   		  String line_token[]=l.split("[ .,?!]+");
                   		  for(j=0;j<line_token.length;j++)
                   		  {
                   			  keywords.add(line_token[j]);
                   			  
                   		  }
                         }
                     }

          /*To identify the names of interface and classes and neglecting the import statements and putting it in a list*/  
                     
                     for(k=0;k < keywords.size();k++)
                     {
                   	  if(keywords.get(k).contains("class"))
                   	  {
                   		classIdentifier=keywords.get(k+1);
                   		  
                   		  if(!sourceBuffer.contains("class" + " " + classIdentifier))
                   		  {
                   			classlist.add(classIdentifier);
                   		sourceBuffer = sourceBuffer + "class" + " " + classIdentifier  + "\n";
                   		
                   		  }

                   	  }
                   	  
                   	  
                   	  if(keywords.get(k).contains("interface"))
                   	  {
                   		classIdentifier=keywords.get(k+1);
                   		  
                   		  if(!sourceBuffer.contains("interface" + " " + classIdentifier))
                   		  {
                   			interfacelist.add(classIdentifier);  
                   		  
                   		sourceBuffer = sourceBuffer + "interface" + " " + classIdentifier + "\n";
                   		  }
                   	  }
                         
                     }

					/* buffer to save the names of all the files with collection appended for dependency checking */
					for (m= 0; m < allfiles.length; m++) {

						String  find= allfiles[m].getName();
						find = find.replaceAll(".java", "");
						buffer.add(find);
						find="Collection<"+find+">";
						buffer.add(find);
					}

					
					new AttributeVisitor().visit(cu,"");
					new MultiplicityVisitor().visit(cu, "");
					new MethodVisitor1().visit(cu, "");
					new ClassVisitor().visit(cu, "");
					new InterfaceVisitor().visit(cu, "");
					new DependancyVisitor().visit(cu, "");
					new ConstructorVisitor().visit(cu, "");
					
				}
			}
			System.out.println("ClassList"+classlist);
			System.out.println("InterfaceList"+interfacelist);
			sourceBuffer += "@enduml\n";
			plantumlFeeder(sourceBuffer,args[0],args[1]);
			//plantumlFeeder(sourceBuffer);
			System.out.println(sourceBuffer); 
		}
	}

	/*visit method with field parameter to get the attribites of the classes with specifiers using modifiers */
	
	@SuppressWarnings("rawtypes")
	private static class AttributeVisitor extends VoidVisitorAdapter {
		public void visit(FieldDeclaration n, Object arg) {
			String k =n.toString().replaceAll("[;]", "");
			String[] accspec = k.split("\\s+");

			if(n.getModifiers()==2 || accspec[0].equals(""))
			{
				privateatt.add(accspec[2]);
				sourceBuffer += classIdentifier + " : " + "-" + " " + accspec[2] + ":" + accspec[1] + "\n";
			}

			if(n.getModifiers()==1 )
			{
				sourceBuffer += classIdentifier + " : " + "+" + " " + accspec[2] + ":" + accspec[1] + "\n";
			}

			if(n.getModifiers()==4)
			{
				//sourceBuffer += classIdentifier + " : " + accspec[0] + " " + "+" + ":" + accspec[1] + "\n";
			}

		}
	}
	
	
	/*visit method with field parameter overwritten to find the one to many relationships */
	
	private static class MultiplicityVisitor extends VoidVisitorAdapter {

		@Override
		public void visit(FieldDeclaration n, Object arg) {
	     	

        	String x = n.getType().toString();
        	String  collec ="";
        	int i;
        	if(buffer.contains(x))
	        	{
        		if(x.contains("Collection"))
        		{
        		   i= buffer.indexOf(x)-1;
        		   collec = "yes";
        		}
        		else
        		{
        			i = buffer.indexOf(x);
        			collec = "no";
        		}
        		
        		if(!sourceBuffer.contains(buffer.get(i) + " -- "  + classIdentifier ))
        		{
        			//sourceBuffer+= buffer.get(i) + " -- "  + classIdentifier +"\n";
        		

        		switch(collec)
        		{
        		case "no":
        		{
        		if(!sourceBuffer.contains(buffer.get(i) + " - \"1\" "  + classIdentifier) && 
        				!sourceBuffer.contains(buffer.get(i) + " - \"*\" "  + classIdentifier))
        			
        				sourceBuffer += classIdentifier + " - \"1\" "  + buffer.get(i) + "\n";
        			
        		}
        		break;
        		case "yes":
        		{
        			if(!sourceBuffer.contains(buffer.get(i) + " - \"*\" "  + classIdentifier) &&
        					!sourceBuffer.contains(buffer.get(i) + " - \"1\" "  + classIdentifier))
        				
        				sourceBuffer += classIdentifier + " - \"*\" "  + buffer.get(i) + "\n";
        			

        		}
        		break;
	        	}
        		}
	        	}
			super.visit(n, arg);
		}


	}

	private static class MethodVisitor1 extends VoidVisitorAdapter {


		@Override
		public void visit(MethodDeclaration n, Object arg) {
	        
			if(n.getParameters()!=null && n.getBody()!=null)
			{
				for(Parameter x : n.getParameters())
				{
					String cn =x.getType().toString();
					if(buffer.contains(cn) )
					{
	
						/*uses dependency shall be carried out only between interface to Classes */
						
						if(!sourceBuffer.contains(x.getType().toString() + "<.. "  + classIdentifier + ":uses") && interfacelist.contains(cn))
						{
							sourceBuffer = sourceBuffer + x.getType().toString() + "<.. "  + classIdentifier + ":uses" + "\n";
						}
					}
				} 
				
				/*getter and setter implementation for making the private attribute public */
				
				if(!n.getName().startsWith("get") && !n.getName().startsWith("set") && n.getModifiers() == 1)
				{
					if(!sourceBuffer.contains(classIdentifier + " : " + "+" +" "+ n.getName() + "("+n.getParameters() +")" + " : " +n.getType()))
					sourceBuffer = sourceBuffer + classIdentifier + " : " + "+" +" "+ n.getName() + "("+n.getParameters() +")" + " : " +n.getType()+"\n";
				}
			}
			else{
				if(!n.getName().startsWith("get") && !n.getName().startsWith("set") && n.getModifiers() == 1)
				{
					if(!sourceBuffer.contains(classIdentifier + " : " + "+" +" "+ n.getName() + "()"+ " : " + n.getType()))
					sourceBuffer = sourceBuffer + classIdentifier + " : " + "+" +" "+ n.getName() + "()" + " : " + n.getType() +"\n";
				}
			}

			if(n.getName().startsWith("get") || n.getName().startsWith("set"))
			{
				for(int i=0;i<privateatt.size();i++)
				{
					String temp = privateatt.get(i);
					temp = temp.replace(temp.charAt(0), Character.toUpperCase(temp.charAt(0)));
					System.out.println(temp+ "name:" + n.getName().toString());
					if(n.getName().toString().contains(temp) || n.getName().toString().contains(privateatt.get(i)))
					{
						sourceBuffer= sourceBuffer.replace( "-" + " " + privateatt.get(i) ,"+" + " " + privateatt.get(i) );
					}

				}
			}        
		}
	}
	
	/*To map all the classes which extends the main class*/
	
	private static class ClassVisitor extends VoidVisitorAdapter {
		@Override
		public void visit(ClassOrInterfaceDeclaration decl, Object arg)
		{

			List<ClassOrInterfaceType> cnames = decl.getExtends();
			if(cnames==null){
				return;
			}
			else{
				extendslist.addAll(cnames);
			}
				for (ClassOrInterfaceType ext : cnames) {
					sourceBuffer += classIdentifier + " " + "--|>" + " " + ext.toString() + "\n";
				}
		}

	}

	
	/*To map all the classes which implements the interface */
	
	private static class InterfaceVisitor extends VoidVisitorAdapter {


		@Override
		public void visit(ClassOrInterfaceDeclaration decl, Object arg)
		{
			List<ClassOrInterfaceType> inames = decl.getImplements();
			if(inames==null){
				return;
			}
			else{
			implementslist.addAll(inames);
			}
				for (ClassOrInterfaceType impl : inames) {
					
					if(!sourceBuffer.contains( impl.toString() + "-0)-"+" " + classIdentifier) && interfacelist.contains(impl.toString()))
						//interfacelist.get(0);
						sourceBuffer += interfacelist.get(0) +  "-0)-" + " " + classIdentifier +  "\n";
				}
		}

	}
	
	/* to get the dependency conditions inside the body declarations */
	
	private static class DependancyVisitor extends VoidVisitorAdapter {


		@Override
		public void visit(MethodDeclaration dp, Object arg) {	
			if (dp.getBody() !=null && dp.getBody().getStmts()!=null) {

				for(Statement stmt : dp.getBody().getStmts())
				{
					if(stmt!=null)
					{
						String[] temp = stmt.toString().split("[ .,?!]+");
						if(temp[0]!=null)
						{
							if(buffer.contains(temp[0]))
								sourceBuffer += classIdentifier + "..>" + temp[0] + ":uses" +"\n";
						}
					}
				}

				
			}
			if(dp.getParameters()!=null && dp.getModifiers()!=2 && !dp.getName().startsWith("get") && !dp.getName().startsWith("set"))
			{
			if(!sourceBuffer.contains(classIdentifier + " : " + "+" +" " + dp.getName() ))
			sourceBuffer += classIdentifier + " : " + "+" +" " + dp.getName() + "("+ dp.getParameters()+")"+ " : " +dp.getType().toString() + "\n";
			}
			else if(dp.getModifiers()!=2 && !dp.getName().startsWith("get") && !dp.getName().startsWith("set"))
			{
				if(!sourceBuffer.contains(classIdentifier + " : " + "+"+" " + dp.getName() ))
					sourceBuffer += classIdentifier + " : " + "+" +" " + dp.getName() + "()"+ " : " +dp.getType().toString() + "\n";
			}
			//if(dp.getModifiers()==1)
				


		}
	}
	
	
	/* checking if the dependency is between interface and classes and getting the constructor names */
	
	private static class ConstructorVisitor extends VoidVisitorAdapter {


		@Override
		public void visit(ConstructorDeclaration n, Object arg) {	
			if(n.getParameters()!=null)
			{
				for(Parameter x : n.getParameters())
				{
					String ipclass =  x.getType().toString();
					if(buffer.contains(ipclass))
					{
						if(interfacelist.contains(ipclass) && !sourceBuffer.contains(ipclass + "<.. "  + classIdentifier + ":uses"))
							sourceBuffer += ipclass + "<.. "  + classIdentifier + ":uses" + "\n";
					}
				}
				if(!sourceBuffer.contains(classIdentifier + " : " + "+" + n.getName()))
				sourceBuffer += classIdentifier + " : " + "+" + n.getName() + "("+n.getParameters() +")" + "\n";
			}
			else
			{
			if(n.getModifiers()==1) 
			{
				sourceBuffer += classIdentifier + " : " + "+" + n.getName() + "()" + "\n";
			}
			}
		}
	}


	/*Function to get the input string in the plantuml format and save it in the output file*/

	public static  void plantumlFeeder(String source,String args0,String args1) {

		File png = null;
		png = new File(args0+args1);
		SourceStringReader reader = new SourceStringReader(source);
		try {
			reader.generateImage(png);
		} catch (IOException e) {
			e.printStackTrace();
		}
		Desktop dt = Desktop.getDesktop();
		   try {
			dt.open(png);
		} catch (IOException e) {

			e.printStackTrace();
		}
	}
/*
	public static  void plantumlFeeder(String source) {

	File png = null;
	png = new File("/Users/manojravi/Desktop/uml/4.jpeg");
	SourceStringReader reader = new SourceStringReader(source);
	try {
		reader.generateImage(png);
	} catch (IOException e) {
		e.printStackTrace();
	}
	Desktop dt = Desktop.getDesktop();
	   try {
		dt.open(png);
	} catch (IOException e) {

		e.printStackTrace();
	}
}*/
}