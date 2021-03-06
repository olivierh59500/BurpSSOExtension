/**
 * EsPReSSO - Extension for Processing and Recognition of Single Sign-On Protocols.
 * Copyright (C) 2015 Tim Guenther and Christian Mainka
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package de.rub.nds.burp.utilities.protocols;

import burp.IBurpExtenderCallbacks;
import burp.IHttpRequestResponse;
import burp.IParameter;
import burp.IRequestInfo;
import de.rub.nds.burp.utilities.Logging;
import static de.rub.nds.burp.utilities.protocols.SSOProtocol.getIDOfLastList;
import static de.rub.nds.burp.utilities.protocols.SSOProtocol.newProtocolflowID;
import java.util.ArrayList;
import java.util.List;

/**
 * Facebook Connect
 * @author Tim Guenther
 * @version 1.0
 */
public class FacebookConnect extends SSOProtocol{
    
    /**
     * {@value #ID}
     */
    public static final String ID = "app_id";
    
    /**
     * Create a new Facebook Connect object.
     * @param message The http message.
     * @param protocol The protocol name.
     * @param callbacks {@link burp.IBurpExtenderCallbacks}
     */
    public FacebookConnect(IHttpRequestResponse message, String protocol, IBurpExtenderCallbacks callbacks){
        super(message, protocol, callbacks);
        super.setToken(findToken());
        super.setProtocolflowID(analyseProtocol());
        add(this, getProtocolflowID());
    }

    /**
     * Analyse the protocol for the right table.
     * @return The protocol flow id.
     */
    @Override
    public int analyseProtocol() {
        logging.log(getClass(), "\nAnalyse: "+getProtocol()+" with ID: "+getToken(), Logging.DEBUG);
        ArrayList<SSOProtocol> last_protocolflow = SSOProtocol.getLastProtocolFlow();
        if(last_protocolflow != null){
            double listsize = (double) last_protocolflow.size();
            double protocol = 0;
            double token = 0;
            
            long tmp = 0;
            long curr_time = 0;
            long last_time = 0;
            boolean wait = true;
            
            for(SSOProtocol sso : last_protocolflow){
                if(sso.getProtocol().contains(this.getProtocol())){
                    logging.log(getClass(), sso.getProtocol(), Logging.DEBUG);
                    protocol++;
                }
                if(sso.getToken().equals(this.getToken())){
                    logging.log(getClass(), sso.getToken(), Logging.DEBUG);
                    token++;
                }
                if(wait){
                    wait = false;
                } else {
                    curr_time = sso.getTimestamp();
                    tmp += curr_time-last_time;
                    logging.log(getClass(), "Diff: "+(curr_time-last_time), Logging.DEBUG);
                }
                last_time = sso.getTimestamp();
            }
            
            if(listsize >= 0){
                double diff_time = ((double)tmp/listsize);
                double curr_diff_time = getTimestamp() - last_protocolflow.get(last_protocolflow.size()-1).getTimestamp();
                double time_bonus = 0;
                logging.log(getClass(), "CurrDiff:"+curr_diff_time+" Diff:"+diff_time, Logging.DEBUG);
                if(curr_diff_time <= (diff_time+4000)){
                    time_bonus = 0.35;
                }
                double prob = ((protocol/listsize)+(token/listsize)*2)/3+(time_bonus);
                logging.log(getClass(), "Probability: "+prob, Logging.DEBUG);
                if(prob >= 0.7){
                    return getIDOfLastList();
                }
            }
            
        }
        return newProtocolflowID();
    }

    /**
     * URL decode the input.
     * @param input The plain data.
     * @return The decoded data.
     */
    @Override
    public String decode(String input) {
        return getHelpers().urlDecode(input);
    }

    /**
     * Find the token associated to the request/response.
     * @return The token.
     */
    @Override
    public String findToken() {
        IRequestInfo iri = super.getCallbacks().getHelpers().analyzeRequest(getMessage());
        List<IParameter> list = iri.getParameters();
        for(IParameter p : list){
            if(p.getName().equals(OAuth.ID)){
                return decode(p.getValue());
            }
            if(p.getName().equals(ID)){
                return decode(p.getValue());
            }
        }
        return "Not Found!";
    }
    
}
