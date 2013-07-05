package lombok.core.handlers;

import static lombok.ast.AST.FieldDecl;
import static lombok.ast.AST.Name;
import static lombok.ast.AST.Type;

import java.util.HashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.ast.AST;
import lombok.ast.Call;
import lombok.ast.IField;
import lombok.ast.IMethod;
import lombok.ast.IType;
import lombok.ast.StringLiteral;
import lombok.ast.TypeRef;
import lombok.core.DiagnosticsReceiver;

import com.doctusoft.bean.Attribute;
import com.doctusoft.bean.Attributes;

@RequiredArgsConstructor
public final class AttributeHandler<TYPE_TYPE extends IType<METHOD_TYPE, ?, ?, ?, ?, ?>, METHOD_TYPE extends IMethod<TYPE_TYPE, ?, ?, ?>, FIELD_TYPE extends IField<TYPE_TYPE, ?, ?, ?>> {
	private final TYPE_TYPE type;
	private final METHOD_TYPE method;
	private final FIELD_TYPE field;
	private final DiagnosticsReceiver diagnosticsReceiver;
	
	private static final Map<String,String> primitives;
	
	static {
		primitives = new HashMap<String,String>(9);
		primitives.put("boolean", "Boolean");
		primitives.put("byte", "Byte");
		primitives.put("char", "Character");
		primitives.put("double", "Double");
		primitives.put("float", "Float");
		primitives.put("int", "Integer");
		primitives.put("long", "Long");
		primitives.put("short", "Short");
		primitives.put("void", "Void");
	}
	
	public void handle() {
		
		if( field != null ) {
			createAttribute(type, field);
		}
		else if (method != null ) {
			createAttribute(type, method);
		}
		else {
			diagnosticsReceiver.addError("Ismeretlen hiba!");
		}
		
	}

	private void createAttribute( final TYPE_TYPE type, final FIELD_TYPE field ) {
		injectAttribute(type, field.type(), field.filteredName());
	}
	
	private void createAttribute(final TYPE_TYPE type, final METHOD_TYPE method) {
		final String fieldName = method.name();
		
		String attributeName;
		if( fieldName.substring(0, 3).equals("get")) {
			attributeName = Character.toLowerCase(fieldName.charAt(3) ) + fieldName.substring(4);
		}
		else if (fieldName.substring(0, 2).equals("is")) {
			attributeName = Character.toLowerCase(fieldName.charAt(2) ) + fieldName.substring(3);
		}
		else {
			diagnosticsReceiver.addError("Nem getter metóduson van.");
			return;
		}

		final TypeRef returnType = method.returns();
		injectAttribute(type, returnType, attributeName);
	}
	
	
	
	private void injectAttribute( final TYPE_TYPE type, final TypeRef attributeType, final String attributeName) {
		final String typeName = attributeType.getTypeName();
		final Call createAttribute = new Call( Name( Attributes.class), "of" )
			.withArgument(AST.ClassLiteral(type.qualifiedName()))
			.withArgument(new StringLiteral(attributeName))
			.withArgument(AST.ClassLiteral(typeName));
		
		final TypeRef attributeTypeRef = Type(Attribute.class)
				.withTypeArgument(Type(type.qualifiedName()))
				.withTypeArgument(Type(getAttributeTypeName(typeName)));
		
		type.editor().injectField( FieldDecl(attributeTypeRef, "_" + attributeName)
				.withInitialization(createAttribute)
				.makeFinal().makeStatic().makePublic());
	}
	
	private String getAttributeTypeName( final String typeName) {
		final char firstChar = typeName.charAt(0);
		if( Character.isLowerCase(firstChar) ) {
			final String attributeTypeName = primitives.get(typeName);
			if( attributeTypeName != null) {
				return attributeTypeName;
			}
		}
		return typeName;
	}
}
