/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.notifications.email;

import java.io.Serializable;

/**
 * @since 2.10
 */
public class EmailMessage implements Serializable {

  private String from;
  private String to;
  private String subject;
  private String message;
  private String messageId;

  /**
   * @param from full name of user, who initiated this message or null, if message was initiated by Sonar
   */
  public EmailMessage setFrom(String from) {
    this.from = from;
    return this;
  }

  /**
   * @see #setFrom(String)
   */
  public String getFrom() {
    return from;
  }

  /**
   * @param to email address where to send this message
   */
  public EmailMessage setTo(String to) {
    this.to = to;
    return this;
  }

  /**
   * @see #setTo(String)
   */
  public String getTo() {
    return to;
  }

  /**
   * @param subject message subject
   */
  public EmailMessage setSubject(String subject) {
    this.subject = subject;
    return this;
  }

  /**
   * @see #setSubject(String)
   */
  public String getSubject() {
    return subject;
  }

  /**
   * @param message message body
   */
  public EmailMessage setMessage(String message) {
    this.message = message;
    return this;
  }

  /**
   * @see #setMessage(String)
   */
  public String getMessage() {
    return message;
  }

  /**
   * @param messageId id of message for threading
   */
  public EmailMessage setMessageId(String messageId) {
    this.messageId = messageId;
    return this;
  }

  /**
   * @see #setMessageId(String)
   */
  public String getMessageId() {
    return messageId;
  }

}
