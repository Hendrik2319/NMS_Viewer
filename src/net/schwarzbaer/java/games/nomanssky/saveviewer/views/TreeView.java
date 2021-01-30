package net.schwarzbaer.java.games.nomanssky.saveviewer.views;

import java.awt.Color;
import java.awt.Component;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;

import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.NVExtra;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.VExtra;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.ArrayValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.JSON_Object;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.NamedValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.ObjectValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.Value;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.Value.Type;

public class TreeView {

	public static class CompareTreeNode extends AbstractDataTreeNode<CompareTreeNode> {
		
		enum Source { First, Second, Both }
		enum Equal{ Equal, UnEqual, Unknown }
		
		private final Value<NVExtra,VExtra> data2;
		private final Source source;
		private Equal allChildrenAreEqual;

		public CompareTreeNode(JSON_Object<NVExtra,VExtra> json_Object1, JSON_Object<NVExtra,VExtra> json_Object2) {
			this(null,null,
				SaveGameData.createObjectValue(json_Object1),
				SaveGameData.createObjectValue(json_Object2));
		}

		private CompareTreeNode(CompareTreeNode parent, String name, Value<NVExtra,VExtra> value, Source source) {
			super(parent,name,value);
			this.data2 = null;
			this.source = source;
			this.allChildrenAreEqual = (source==Source.Both)?Equal.Equal:Equal.UnEqual;
		}

		private CompareTreeNode(CompareTreeNode parent, String name, Value<NVExtra,VExtra> value1, Value<NVExtra,VExtra> value2) {
			super(parent,name,value1);
			this.data2 = value2;
			this.source = Source.Both;
			this.allChildrenAreEqual = Equal.Unknown;
			if (value1.type!=Type.Array && value1.type!=Type.Object) throw new IllegalArgumentException("Construction of double value CompareTreeNode with value type \""+value1.type+"\" is not allowed.");
			if (value2.type!=Type.Array && value2.type!=Type.Object) throw new IllegalArgumentException("Construction of double value CompareTreeNode with value type \""+value2.type+"\" is not allowed.");
		}

		private boolean allChildrenAreEqual() {
			if (children == null) createChildren();
			if (allChildrenAreEqual == Equal.Unknown) {
				for (CompareTreeNode child:children)
					if (!child.allChildrenAreEqual()) {
						allChildrenAreEqual = Equal.UnEqual;
						break;
					}
				if (allChildrenAreEqual == Equal.Unknown)
					allChildrenAreEqual = Equal.Equal;
			}
			return allChildrenAreEqual == Equal.Equal;
		}

		@Override
		void createChildren() {
			switch (data.type) {
			case Object: {
				ObjectValue<NVExtra,VExtra> object1 = data ==null ? null : data .castToObjectValue();
				ObjectValue<NVExtra,VExtra> object2 = data2==null ? null : data2.castToObjectValue();
				Vector<CompareTreeNode> childrenVec = new Vector<CompareTreeNode>();
				
				if (object2==null) {
					for (NamedValue<NVExtra,VExtra> namedvalue : object1.value)
						childrenVec.add(new CompareTreeNode(this,namedvalue.name,namedvalue.value,source));
				} else {
					HashMap<String, Value<NVExtra,VExtra>> object2Values = new HashMap<>();
					for (NamedValue<NVExtra,VExtra> namedvalue : object2.value)
						object2Values.put(namedvalue.name, namedvalue.value);
					
					for (NamedValue<NVExtra,VExtra> namedvalue : object1.value) {
						String valueName = namedvalue.name;
						
						if (!object2Values.containsKey(valueName)) {
							childrenVec.add(new CompareTreeNode(this,valueName,namedvalue.value,Source.First));
						} else {
							Value<NVExtra,VExtra> value1 = namedvalue.value;
							Value<NVExtra,VExtra> value2 = object2Values.get(valueName);
							object2Values.remove(valueName);
							if (areEqual(value1,value2)) {
								if (value1.type == Type.Object || value1.type == Type.Array)
									childrenVec.add(new CompareTreeNode(this,valueName,value1,value2));
								else
									childrenVec.add(new CompareTreeNode(this,valueName,value1,Source.Both));
							} else {
								childrenVec.add(new CompareTreeNode(this,valueName,value1,Source.First));
								childrenVec.add(new CompareTreeNode(this,valueName,value2,Source.Second));
							}
						}
					}
					for (String valueName : object2Values.keySet()) {
						Value<NVExtra,VExtra> value2 = object2Values.get(valueName);
						childrenVec.add(new CompareTreeNode(this,valueName,value2,Source.Second));
					}
				}
				
				children = childrenVec.toArray(new CompareTreeNode[0]);
			} break;
			case Array: {
				ArrayValue<NVExtra,VExtra> array1 = data ==null ? null : data .castToArrayValue();
				ArrayValue<NVExtra,VExtra> array2 = data2==null ? null : data2.castToArrayValue();
				Vector<CompareTreeNode> childrenVec = new Vector<CompareTreeNode>();
				
				if (array2==null) {
					for (Value<NVExtra,VExtra> value1 : array1.value)
						childrenVec.add(new CompareTreeNode(this,null,value1,source));
				} else {
					int size1 = array1.value.size();
					int size2 = array2.value.size();
					int size = Math.max(size1, size2);
					for (int i=0; i<size; ++i)
						if      (i>=size1) childrenVec.add(new CompareTreeNode(this,null,array2.value.get(i),Source.Second));
						else if (i>=size2) childrenVec.add(new CompareTreeNode(this,null,array1.value.get(i),Source.First ));
						else {
							Value<NVExtra,VExtra> value1 = array1.value.get(i);
							Value<NVExtra,VExtra> value2 = array2.value.get(i);
							if (areEqual(value1,value2)) { 
								if (value1.type == Type.Object || value1.type == Type.Array)
									childrenVec.add(new CompareTreeNode(this,null,value1,value2));
								else
									childrenVec.add(new CompareTreeNode(this,null,value1,Source.Both));
							} else {
								childrenVec.add(new CompareTreeNode(this,null,value1,Source.First));
								childrenVec.add(new CompareTreeNode(this,null,value2,Source.Second));
							}
						}
				}
				
				children = childrenVec.toArray(new CompareTreeNode[0]);
			} break;
			default:
				children = new CompareTreeNode[0];
				break;
			
			}
		}

		private static boolean areEqual(Value<NVExtra,VExtra> value1, Value<NVExtra,VExtra> value2) {
			if (value1.type != value2.type) return false;
			switch(value1.type) {
			case String : return value1.castToStringValue().value.compareTo(value2.castToStringValue().value)==0;
//			{
//				String str1 = ((StringValue)value1).value;
//				String str2 = ((StringValue)value2).value;
//				if (str1==null) return str2==null;
//				return str1.compareTo(str2)==0;
//			}
			case Bool   : return value1.castToBoolValue   ().value.booleanValue() == value2.castToBoolValue   ().value.booleanValue();
			case Float  : return value1.castToFloatValue  ().value.doubleValue()  == value2.castToFloatValue  ().value.doubleValue();
			case Integer: return value1.castToIntegerValue().value.longValue()    == value2.castToIntegerValue().value.longValue();
			case Array  : return true;
			case Object : return true;
			case Null   : return true;
			}
			return false;
		}
		
		
		
//		@Override
//		public String toString() {
//			switch(source) {
//			case First : return "1| "+super.toString();
//			case Second: return "2| "+super.toString();
//			default    :
//				if (allChildrenAreEqual()) return super.toString();
//				else return "## "+super.toString();
//			}
//		}

		@Override
		protected String dataToString() {
			if (data2==null)
				return super.dataToString();
			
			switch(data.type) {
			case Array  : {
				int size  = data .castToArrayValue().value.size();
				int size2 = data2.castToArrayValue().value.size();
				if (size == size2) return String.format("[%d]", size);
				return String.format("[%d] | [%d]", size, size2);
			}
			case Object : {
				int size  = data .castToObjectValue().value.size();
				int size2 = data2.castToObjectValue().value.size();
				if (size == size2) return String.format("{%d}", size);
				return String.format("{%d} | {%d}", size, size2);
			}
			default:
				return super.dataToString();
			}
		}

		public static class CellRenderer extends DefaultTreeCellRenderer {
			private static final long serialVersionUID = -7036052917753626469L;
		
			private static final Color FIRST   = new Color(0xFF0000);
			private static final Color SECOND  = new Color(0x009F00);
			private static final Color UNEQUAL = new Color(0x9F009F);
			
			@Override
			public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
				Component component = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
				if (value instanceof CompareTreeNode) {
					CompareTreeNode node = (CompareTreeNode)value;
					switch (node.source) {
					case First : component.setForeground(FIRST); break;
					case Second: component.setForeground(SECOND); break;
					case Both:
						if (!node.allChildrenAreEqual()) component.setForeground(UNEQUAL);
						break;
					}
				}
				return component;
			}
		}
		
		
	}

	static class JsonTreeNode extends AbstractDataTreeNode<JsonTreeNode> {

//		public JsonTreeNode(JSON_Object data) {
//			this(null,null,new ObjectValue(data));
//		}
//
//		public JsonTreeNode(JSON_Array data) {
//			this(null,null,new ArrayValue(data));
//		}

		private boolean hideProcessedNodes;
		boolean wasDeObfuscated;

		private JsonTreeNode(JsonTreeNode parent, String name, Value<NVExtra,VExtra> value, boolean wasDeObfuscated, boolean hideProcessedNodes) {
			super(parent,name,value);
			this.wasDeObfuscated = wasDeObfuscated;
			this.hideProcessedNodes = hideProcessedNodes;
		}

		public JsonTreeNode(JSON_Object<NVExtra,VExtra> data, boolean hideProcessedNodes) {
			this(null,null,
				SaveGameData.createObjectValue(data),
				true, hideProcessedNodes);
		}

		@Override
		void createChildren() {
			switch (data.type) {
			case Object:
				if (data instanceof ObjectValue) {
					ObjectValue<NVExtra,VExtra> objectValue = data.castToObjectValue();
					children = new JsonTreeNode[objectValue.value.size()];
					int i=0;
					for (NamedValue<NVExtra,VExtra> namedvalue : objectValue.value)
						if (!hideProcessedNodes || !namedvalue.value.extra.wasProcessed || namedvalue.value.extra.hasUnprocessedChildren())
							children[i++] = new JsonTreeNode( this, namedvalue.name, namedvalue.value, namedvalue.extra.wasDeObfuscated, hideProcessedNodes );
					children = Arrays.copyOf(children, i);
				} else
					throw new IllegalStateException("Found a Value with type==Object, but not instance of ObjectValue");
				break;
			case Array:
				if (data instanceof ArrayValue) {
					ArrayValue<NVExtra,VExtra> arrayValue = data.castToArrayValue();
					children = new JsonTreeNode[arrayValue.value.size()];
					int i=0;
					for (Value<NVExtra,VExtra> value : arrayValue.value)
						if (!hideProcessedNodes || !value.extra.wasProcessed || value.extra.hasUnprocessedChildren())
							children[i++] = new JsonTreeNode(this,null,value, true, hideProcessedNodes);
					children = Arrays.copyOf(children, i);
				} else
					throw new IllegalStateException("Found a Value with type==Array, but not instance of ArrayValue");
				break;
			default:
				children = new JsonTreeNode[0];
				break;
			
			}
		}
	}

	abstract static class AbstractDataTreeNode<TreeNodeType extends AbstractDataTreeNode<TreeNodeType>> extends AbstractTreeNode<TreeNodeType> {
		
		protected final String name;
		protected final Value<NVExtra,VExtra> data;
		
		public AbstractDataTreeNode(TreeNodeType parent, String name, Value<NVExtra,VExtra> data) {
			super(parent);
			this.name = name;
			this.data = data;
		}

		@Override
		public String toString() {
			return toString(dataToString());
		}

		public String toString(String dataStr) {
			if (name==null) return dataStr;
			return name +" : "+ dataStr;
		}

		protected String dataToString() {
			switch(data.type) {
			case String : return String.format("\"%s\"", data.castToStringValue ().value);
			case Bool   : return String.format("%s"    , data.castToBoolValue   ().value);
			case Float  : return                      ""+data.castToFloatValue  ().value ;
			case Integer: return String.format("%d"    , data.castToIntegerValue().value);
			case Array  : return String.format("[%d]"  , data.castToArrayValue  ().value.size());
			case Object : return String.format("{%d}"  , data.castToObjectValue ().value.size());
			case Null   : return "<null>";
			}
			return data.toString();
		}
		
		@Override
		public boolean getAllowsChildren() {
			return (data.type == Type.Object) || (data.type == Type.Array);
		}
		
	}

	abstract static class AbstractTreeNode<TreeNodeType extends TreeNode> implements TreeNode {
		
		protected final TreeNodeType parent;
		protected TreeNodeType[] children;
		
		public AbstractTreeNode(TreeNodeType parent) {
			this.parent = parent;
			this.children = null;
		}
		
		@Override
		abstract public String toString();
		abstract void createChildren();

		@Override
		public TreeNode getParent() {
			return parent;
		}
		
		public TreeNodeType[] getChildren() {
			if (children == null) createChildren();
			return children;
		}
		
		@Override
		public int getChildCount() {
			if (children == null) createChildren();
			return children.length;
		}
		
		@Override
		public boolean isLeaf() {
			return getChildCount()==0;
		}
	
		@Override
		public int getIndex(TreeNode node) {
			if (children == null) createChildren();
			//return Arrays.binarySearch(children, node);
			for (int i=0; i<children.length; ++i)
				if (children[i]==node) return i;
			return -1;
		}

		@Override
		public TreeNode getChildAt(int childIndex) {
			if (children == null) createChildren();
			return children[childIndex];
		}
	
		@SuppressWarnings("rawtypes")
		@Override
		public Enumeration children() {
			if (children == null) createChildren();
			return new Enumeration<TreeNode>() {
				int index = 0;
				@Override public boolean hasMoreElements() { return children.length>index; }
				@Override public TreeNode nextElement() { return children[index++]; }
			};
		}
	}
}
