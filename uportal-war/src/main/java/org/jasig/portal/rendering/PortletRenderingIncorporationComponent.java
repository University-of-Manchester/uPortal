/**
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.jasig.portal.rendering;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jasig.portal.character.stream.CharacterEventReader;
import org.jasig.portal.character.stream.FilteringCharacterEventReader;
import org.jasig.portal.character.stream.events.CharacterDataEventImpl;
import org.jasig.portal.character.stream.events.CharacterEvent;
import org.jasig.portal.character.stream.events.PortletContentPlaceholderEvent;
import org.jasig.portal.character.stream.events.PortletHeaderPlaceholderEvent;
import org.jasig.portal.character.stream.events.PortletTitlePlaceholderEvent;
import org.jasig.portal.portlet.rendering.IPortletExecutionManager;
import org.jasig.portal.utils.cache.CacheKey;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Inserts the results of portlet's rendering into the character stream
 * 
 * @author Eric Dalquist
 * @version $Revision$
 */
public class PortletRenderingIncorporationComponent extends CharacterPipelineComponentWrapper {
    private IPortletExecutionManager portletExecutionManager;
    
    @Autowired
    public void setPortletExecutionManager(IPortletExecutionManager portletExecutionManager) {
        this.portletExecutionManager = portletExecutionManager;
    }

    /* (non-Javadoc)
     * @see org.jasig.portal.rendering.PipelineComponent#getCacheKey(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    public CacheKey getCacheKey(HttpServletRequest request, HttpServletResponse response) {
        /*
         * TODO do all the portlet cache keys need to be included here?
         * Probably for this to be useful
         */
        return this.wrappedComponent.getCacheKey(request, response);
    }

    /* (non-Javadoc)
     * @see org.jasig.portal.rendering.PipelineComponent#getEventReader(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    public PipelineEventReader<CharacterEventReader, CharacterEvent> getEventReader(HttpServletRequest request, HttpServletResponse response) {
        final PipelineEventReader<CharacterEventReader, CharacterEvent> pipelineEventReader = this.wrappedComponent.getEventReader(request, response);
        
        final CharacterEventReader eventReader = pipelineEventReader.getEventReader();
        final PortletIncorporatingEventReader portletIncorporatingEventReader = new PortletIncorporatingEventReader(eventReader, request, response);
        
        final Map<String, String> outputProperties = pipelineEventReader.getOutputProperties();
        return new PipelineEventReaderImpl<CharacterEventReader, CharacterEvent>(portletIncorporatingEventReader, outputProperties);
    }

    private class PortletIncorporatingEventReader extends FilteringCharacterEventReader {
        private final HttpServletRequest request;
        private final HttpServletResponse response;
        
        public PortletIncorporatingEventReader(CharacterEventReader delegate, HttpServletRequest request, HttpServletResponse response) {
            super(delegate);
            this.request = request;
            this.response = response;
        }

        @Override
        protected CharacterEvent filterEvent(CharacterEvent event, boolean peek) {
            switch (event.getEventType()) {
            	case PORTLET_HEADER: {
            		final PortletHeaderPlaceholderEvent headerPlaceholderEvent = (PortletHeaderPlaceholderEvent) event;
            		final String subscribeId = headerPlaceholderEvent.getPortletSubscribeId();
            		
            		final String output = portletExecutionManager.getPortletHeadOutput(subscribeId, this.request, this.response);
            		
            		return new CharacterDataEventImpl(output);
            	}
                case PORTLET_CONTENT: {
                    final PortletContentPlaceholderEvent contentPlaceholderEvent = (PortletContentPlaceholderEvent)event;
                    final String subscribeId = contentPlaceholderEvent.getPortletSubscribeId();
                    
                    final String output = portletExecutionManager.getPortletOutput(subscribeId, this.request, this.response);
                    
                    return new CharacterDataEventImpl(output);
                }
                case PORTLET_TITLE: {
                    final PortletTitlePlaceholderEvent titlePlaceholderEvent = (PortletTitlePlaceholderEvent)event;
                    final String subscribeId = titlePlaceholderEvent.getPortletSubscribeId();
                    
                    final String title = portletExecutionManager.getPortletTitle(subscribeId, this.request, this.response);
                    
                    return new CharacterDataEventImpl(title);
                }
            }

            
            return event;
        }
    }
}