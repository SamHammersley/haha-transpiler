package com.sauljohnson.humoresque.transpiler;

import com.sauljohnson.humoresque.parser.Token;
import com.sauljohnson.humoresque.parser.model.*;

/**
 * Represents a HAHA to Java transpiler.
 *
 * @since 24/10/2019
 * @author Saul Johnson <saul.a.johnson@gmail.com>
 */
public class JavaTranspiler implements Transpiler {

    /**
     * Removes double occurrences of a term from an input string.
     *
     * @param term  the term
     * @param input the input string
     * @return      the input string with double occurrences removed
     */
    private String removeDoubles(String term, String input) {
        String output = input;
        while (output.contains(term + term)) {
            output = output.replace(term + term, term);
        }
        return output;
    }

    /**
     * Removes double newlines from the input string.
     *
     * @param input the input string
     * @return      the input string with double newlines removed
     */
    private String removeDoubleNewlines(String input) {
        return removeDoubles("\n", input);
    }

    /**
     * Removes double spaces from the input string.
     *
     * @param input the input string
     * @return      the input string with double spaces removed
     */
    private String removeDoubleSpaces(String input) {
        return removeDoubles(" ", input);
    }

    /**
     * Concatenates tokens together as a string, optionally transforming them.
     *
     * @param tokens    the tokens to concatenate
     * @param transform whether or not to transform tokens to their Java equivalents during concatenation
     * @param delimiter the delimiter to place between tokens
     * @return          the resulting string
     */
    private String concatTokens(Token[] tokens, boolean transform, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tokens.length; i++) {
            if (i > 0) {
                sb.append(delimiter); // Add delimiter between tokens.
            }
            if (transform) {
                switch (tokens[i].getType()) { // Some tokens need transforming.
                    case CONJUNCTION:
                        sb.append("&&");
                        break;
                    case DISJUNCTION:
                        sb.append("||");
                        break;
                    case EQUALITY:
                        sb.append("==");
                        break;
                    case INEQUALITY:
                        sb.append("!=");
                        break;
                    case NEGATION:
                        sb.append("!");
                        break;
                    case GREATER_THAN_OR_EQUAL_TO:
                        sb.append(">=");
                        break;
                    case LESS_THAN_OR_EQUAL_TO:
                        sb.append("<=");
                        break;
                    default:
                        sb.append(tokens[i].getText()); // Other tokens unchanged.
                        break;
                }
            } else {
                sb.append(tokens[i].getText()); // Skip token transformation.
            }
        }
        return sb.toString();
    }

    /**
     * Concatenates tokens together as a string, optionally transforming them.
     *
     * @param tokens    the tokens to concatenate
     * @param transform whether or not to transform tokens to their Java equivalents during concatenation
     * @return          the resulting string
     */
    private String concatTokens(Token[] tokens, boolean transform) {
        return concatTokens(tokens, transform," ");
    }

    /**
     * Concatenates tokens together as a string.
     *
     * @param tokens    the tokens to concatenate
     * @return          the resulting string
     */
    private String concatTokens(Token[] tokens) {
        return concatTokens(tokens, false);
    }

    /**
     * Transforms a {@link HahaType} into a string representing a Java type.
     *
     * @param type  the type to transform
     * @return      a string representing a Java type
     */
    private String lookupType(HahaType type) {
        StringBuilder sb = new StringBuilder();
        switch(type.getBaseType()){
            case Z:
            case INT:
                sb.append("int"); // Both of these map to the integer data type.
                break;
            case BOOLEAN:
                sb.append("boolean");
                break;
        }
        if (type.isArrayType()) {
            sb.append("[]"); // Annotate with brackets for array types.
        }
        return sb.toString();
    }

    /**
     * Emits a loop.
     *
     * @param sb    the {@link StringBuilder} the program is being built in
     * @param loop  the loop to emit
     */
    private void emitLoop(StringBuilder sb, Loop loop) {
        sb.append("while(")
                .append(concatTokens(loop.getPredicate(), true)) // Emit predicate.
                .append(")");
        emitStatement(sb, loop.getStatement()); // Emit body.
    }

    /**
     * Emits a conditional.
     *
     * @param sb            the {@link StringBuilder} the program is being built in
     * @param conditional   the conditional to emit
     */
    private void emitConditional(StringBuilder sb, Conditional conditional) {
        sb.append("if(")
                .append(concatTokens(conditional.getPredicate(), true)) // Emit predicate.
                .append("){");
        emitStatement(sb, conditional.getTrueArm()); // Emit true arm.
        if (conditional.getFalseArm() != null) {
            sb.append("} else {");
            emitStatement(sb, conditional.getFalseArm()); // Emit false arm if any.
        }
        sb.append("}");
    }

    /**
     * Emits an assignment.
     *
     * @param sb            the {@link StringBuilder} the program is being built in
     * @param assignment    the assignment to emit
     */
    private void emitAssignment(StringBuilder sb, Assignment assignment) {

        // Append identifier.
        sb.append(assignment.getIdentifier());

        // Append indexing syntax if array assignment.
        if (assignment.getIsArrayAssignment()) {
            ArrayAssignment arrayAssignment = (ArrayAssignment) assignment;
            sb.append(arrayAssignment.getIdentifier())
                    .append("[")
                    .append(concatTokens(arrayAssignment.getIndex(), true))
                    .append("]");
        }

        // Append right-hand side.
        sb.append(" = ")
                .append(concatTokens(assignment.getExpression(), true))
                .append(";\n");
    }

    /**
     * Emits a statement.
     *
     * @param sb                    the {@link StringBuilder} the program is being built in
     * @param statement             the statement to emit
     * @param excludeBlockBraces    whether or not to exclude braces around an emitted block
     */
    private void emitStatement(StringBuilder sb, Statement statement, boolean excludeBlockBraces) {

        // Branch based on type of statement.
        switch (statement.getStatementType()) {
            case BLOCK:
                // Cast to block.
                Block block = (Block) statement;

                // Open block.
                if (!excludeBlockBraces) {
                    sb.append("{\n");
                }

                // Go through components in block.
                for (ProgramComponent component : block.getProgramComponents()) {

                    // Deal with annotations here. They can only occur within blocks.
                    switch (component.getProgramComponentType()) {
                        case ANNOTATION:
                            Annotation annotation = (Annotation) component;
                            String annotationString = concatTokens(annotation.getTokens())
                                    .replace("{", "")
                                    .replace("}", "")
                                    .replace("\n", " ");
                            sb.append("/*")
                                    .append(removeDoubleSpaces(annotationString))
                                    .append("*/\n");
                            break;
                        case STATEMENT:
                            emitStatement(sb, (Statement) component); // Emit regular statement.
                            break;
                        default:
                            break;
                    }
                }

                // Close block.
                if (!excludeBlockBraces) {
                    sb.append("}\n");
                }
                break;
            case LOOP:
                emitLoop(sb, (Loop) statement); // Emit loop structure.
                break;
            case CONDITIONAL:
                emitConditional(sb, (Conditional) statement); // Emit conditional statement.
                break;
            case ASSIGNMENT:
                emitAssignment(sb, (Assignment) statement); // Emit assignment.
                break;
            default:
                break;
        }
    }

    /**
     * Emits a statement.
     *
     * @param sb        the {@link StringBuilder} the program is being built in
     * @param statement the statement to emit
     */
    private void emitStatement(StringBuilder sb, Statement statement) {
        emitStatement(sb, statement, false);
    }

    /**
     * Emits a function body.
     *
     * @param sb        the {@link StringBuilder} the program is being built in
     * @param function  the function with the body to emit
     */
    private void emitFunctionBody(StringBuilder sb, Function function) {

        // Emits a function body.
        Variable[] variables = function.getVariables();
        Statement statement = function.getStatement();

        // Function body must be a block.
        sb.append("{\n");

        // Emit variables.
        for (Variable variable : variables) {
            sb.append(lookupType(variable.getType()))
                    .append(" ")
                    .append(variable.getIdentifier())
                    .append(";\n");
        }
        sb.append(lookupType(function.getReturnType())) // Extra variable to carry function value.
                .append(" ")
                .append(function.getIdentifier())
                .append(";\n");

        // Emit function statement.
        emitStatement(sb, statement, true);

        // Return function value.
        sb.append("return ")
                .append(function.getIdentifier())
                .append(";\n");

        // Close body block.
        sb.append("}\n\n");
    }

    /**
     * Emits a function.
     *
     * @param sb        the {@link StringBuilder} the program is being built in
     * @param function  the function to emit
     */
    private void emitFunction(StringBuilder sb, Function function) {

        // Emit type and identifier.
        sb.append(lookupType(function.getReturnType())) // Return type.
                .append(" ")
                .append(function.getIdentifier()) // Function name.
                .append("("); // Begin argument list.

        // Emit argument list.
        Argument[] arguments = function.getArguments();
        for (int i = 0; i < arguments.length; i++) {
            if (i > 0) {
                sb.append(", "); // Comma if needed.
            }
            sb.append(lookupType(arguments[i].getType()))
                    .append(" ")
                    .append(arguments[i].getIdentifier());
        }
        sb.append(")\n");

        // Emit body of function.
        emitFunctionBody(sb, function);
    }

    /**
     * @inheritDoc
     */
    public String transpileFunction(Function function) {

        // Emit function.
        StringBuilder sb = new StringBuilder();
        emitFunction(sb, function);

        // Return emitted function.
        return sb.toString();
    }

    /**
     * @inheritDoc
     */
    public String transpile(Program program) {

        // The string builder to emit to.
        StringBuilder sb = new StringBuilder();

        // Emit axioms (commented).
        for(Axiom axiom : program.getAxioms()) {
            sb.append("/*")
                    .append(concatTokens(axiom.getTokens()))
                    .append("*/\n\n");
        }

        // Emit predicates (commented).
        for(Predicate predicate : program.getPredicates()) {
            sb.append("/*")
                    .append(concatTokens(predicate.getTokens()))
                    .append("*/\n\n");
        }

        // Emit functions.
        for(Function function : program.getFunctions()) {
            emitFunction(sb, function);
        }

        return sb.toString(); // Return generated code.
    }
}
