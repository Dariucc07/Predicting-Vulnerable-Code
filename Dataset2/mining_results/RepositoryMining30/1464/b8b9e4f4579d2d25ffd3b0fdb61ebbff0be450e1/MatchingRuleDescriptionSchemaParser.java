/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.shared.ldap.schema.parsers;


import java.text.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import antlr.RecognitionException;
import antlr.TokenStreamException;


/**
 * A parser for RFC 4512 matching rule descriptions.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class MatchingRuleDescriptionSchemaParser extends AbstractSchemaParser
{
    /** The LoggerFactory used by this class */
    protected static final Logger LOG = LoggerFactory.getLogger( MatchingRuleDescriptionSchemaParser.class );

    /**
     * Creates a schema parser instance.
     */
    public MatchingRuleDescriptionSchemaParser()
    {
    }


    /**
     * Parses a matching rule description according to RFC 4512:
     * 
     * <pre>
     * MatchingRuleDescription = LPAREN WSP
     *    numericoid                 ; object identifier
     *    [ SP "NAME" SP qdescrs ]   ; short names (descriptors)
     *    [ SP "DESC" SP qdstring ]  ; description
     *    [ SP "OBSOLETE" ]          ; not active
     *    SP "SYNTAX" SP numericoid  ; assertion syntax
     *    extensions WSP RPAREN      ; extensions
     * 
     * extensions = *( SP xstring SP qdstrings )
     * xstring = "X" HYPHEN 1*( ALPHA / HYPHEN / USCORE ) 
     * </pre>
     * 
     * @param matchingRuleDescription the matching rule description to be parsed
     * @return the parsed MatchingRuleDescription bean
     * @throws ParseException if there are any recognition errors (bad syntax)
     */
    public synchronized MatchingRuleDescription parseMatchingRuleDescription( String matchingRuleDescription )
        throws ParseException
    {

        if ( matchingRuleDescription == null )
        {
            LOG.error( "Cannot parse a null MatchingRule" );
            throw new ParseException( "Null", 0 );
        }

        reset( matchingRuleDescription ); // reset and initialize the parser / lexer pair

        try
        {
            MatchingRuleDescription mrd = parser.matchingRuleDescription();
            return mrd;
        }
        catch ( RecognitionException re )
        {
            String msg = "Parser failure on matching rule description:\n\t" + matchingRuleDescription +
                "\nAntlr message: " + re.getMessage() +
                "\nAntlr column: " + re.getColumn();
            LOG.error( msg );
            throw new ParseException( msg, re.getColumn() );
        }
        catch ( TokenStreamException tse )
        {
            String msg = "Parser failure on matching rule description:\n\t" + matchingRuleDescription +
                "\nAntlr message: " + tse.getMessage();
            LOG.error( msg );
            throw new ParseException( msg, 0 );
        }
    }


    /**
     * Parses a MatchingRul description
     * 
     * @param The MatchingRul description to parse
     * @return An instance of MatchingRulDescription
     */
    public MatchingRuleDescription parse( String schemaDescription ) throws ParseException
    {
        return parseMatchingRuleDescription( schemaDescription );
    }
}
