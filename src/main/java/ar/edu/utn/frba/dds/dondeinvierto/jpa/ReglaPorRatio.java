package ar.edu.utn.frba.dds.dondeinvierto.jpa;

import java.util.List;

import javax.persistence.Entity;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;

import ar.edu.utn.frba.dds.dondeinvierto.ExpresionInvalidaException;
import ar.edu.utn.frba.dds.dondeinvierto.Operable;
import ar.edu.utn.frba.dds.dondeinvierto.antlr.DondeInviertoLexer;
import ar.edu.utn.frba.dds.dondeinvierto.antlr.DondeInviertoParser;

@Entity
public class ReglaPorRatio extends ReglaAbstracta{

	public ReglaPorRatio(String expresion, int periodo) throws ExpresionInvalidaException {
		super(expresion, periodo);
	}

	@Override
	protected String verificarExpresion(String expresion) throws ExpresionInvalidaException {
		DondeInviertoLexer lexer = new DondeInviertoLexer( new ANTLRInputStream(expresion));
		CommonTokenStream tokens = new CommonTokenStream( lexer );
		DondeInviertoParser parser = new DondeInviertoParser( tokens, MetodosUtiles.obtenerCuentasEindicadoresDeUsuario());
		DondeInviertoParser.IdentificadorContext tree = parser.identificador();
		if(parser.getNumberOfSyntaxErrors()>0)
			throw new ExpresionInvalidaException();
		return expresion;
	}
}
