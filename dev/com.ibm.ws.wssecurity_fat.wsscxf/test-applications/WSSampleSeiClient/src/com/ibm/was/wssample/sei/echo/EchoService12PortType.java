/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

//
// Generated By:JAX-WS RI IBM 2.1.1 in JDK 6 (JAXB RI IBM JAXB 2.1.3 in JDK 1.6)
//

package com.ibm.was.wssample.sei.echo;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlSeeAlso;

@WebService(name = "EchoService12PortType", targetNamespace = "http://com/ibm/was/wssample/sei/echo/")
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@XmlSeeAlso({
              ObjectFactory.class
})
public interface EchoService12PortType {

    /**
     *
     * @param parameter
     * @return
     *         returns com.ibm.was.wssample.sei.echo.EchoStringResponse
     */
    @WebMethod(action = "echoOperation")
    @WebResult(name = "echoStringResponse", targetNamespace = "http://com/ibm/was/wssample/sei/echo/", partName = "parameter")
    public EchoStringResponse echoOperation(
                                            @WebParam(name = "echoStringInput", targetNamespace = "http://com/ibm/was/wssample/sei/echo/",
                                                      partName = "parameter") EchoStringInput parameter);

}
