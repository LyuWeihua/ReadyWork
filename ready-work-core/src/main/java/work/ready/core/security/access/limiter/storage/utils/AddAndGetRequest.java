/**
 * The MIT License
 * Copyright (c) 2016 Coveo
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package work.ready.core.security.access.limiter.storage.utils;

import work.ready.core.security.access.limiter.limit.utils.LimitUtils;

import java.time.Duration;
import java.time.Instant;

public class AddAndGetRequest {
  private String resource;
  private String limitName;
  private String property;
  private boolean distributed;
  private Duration expiration;
  private Instant eventTimestamp;
  private int cost;
  private int limit;

  private Instant bucket;

  public String getResource() {
    return resource;
  }

  public String getLimitName() {
    return limitName;
  }

  public String getProperty() {
    return property;
  }

  public boolean isDistributed() {
    return distributed;
  }

  public Duration getExpiration() {
    return expiration;
  }

  public Instant getEventTimestamp() {
    return eventTimestamp;
  }

  public int getCost() {
    return cost;
  }

  public Instant getBucket() {
    return bucket;
  }

  public int getLimit() {
    return limit;
  }

  private AddAndGetRequest(Builder builder) {
    resource = builder.resource;
    limitName = builder.limitName;
    property = builder.property;
    distributed = builder.distributed;
    expiration = builder.expiration;
    eventTimestamp = builder.eventTimestamp;
    cost = builder.cost;
    limit = builder.limit;
    bucket = LimitUtils.calculateBucket(eventTimestamp, expiration);
  }

  public static final class Builder {
    private String resource;
    private String limitName;
    private String property;
    private boolean distributed;
    private Duration expiration;
    private Instant eventTimestamp;
    private int cost = 1;
    private int limit;

    public Builder() {}

    public Builder(AddAndGetRequest other) {
      this.resource = other.resource;
      this.limitName = other.limitName;
      this.property = other.property;
      this.distributed = other.distributed;
      this.expiration = other.expiration;
      this.eventTimestamp = other.eventTimestamp;
      this.cost = other.cost;
      this.limit = other.limit;
    }

    public Builder withResource(String val) {
      resource = val;
      return this;
    }

    public Builder withLimitName(String val) {
      limitName = val;
      return this;
    }

    public Builder withProperty(String val) {
      property = val;
      return this;
    }

    public Builder withDistributed(boolean val) {
      distributed = val;
      return this;
    }

    public Builder withExpiration(Duration val) {
      expiration = val;
      return this;
    }

    public Builder withEventTimestamp(Instant val) {
      eventTimestamp = val;
      return this;
    }

    public Builder withCost(int val) {
      cost = val;
      return this;
    }

    public Builder withLimit(int val) {
      limit = val;
      return this;
    }

    public AddAndGetRequest build() {
      return new AddAndGetRequest(this);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    AddAndGetRequest that = (AddAndGetRequest) o;

    if (cost != that.cost) return false;
    if (limit != that.limit) return false;
    if (resource != null ? !resource.equals(that.resource) : that.resource != null) return false;
    if (limitName != null ? !limitName.equals(that.limitName) : that.limitName != null)
      return false;
    if (property != null ? !property.equals(that.property) : that.property != null) return false;
    if (expiration != null ? !expiration.equals(that.expiration) : that.expiration != null)
      return false;
    if (eventTimestamp != null
        ? !eventTimestamp.equals(that.eventTimestamp)
        : that.eventTimestamp != null) return false;
    return bucket != null ? bucket.equals(that.bucket) : that.bucket == null;
  }

  @Override
  public int hashCode() {
    int result = resource != null ? resource.hashCode() : 0;
    result = 31 * result + (limitName != null ? limitName.hashCode() : 0);
    result = 31 * result + (property != null ? property.hashCode() : 0);
    result = 31 * result + (expiration != null ? expiration.hashCode() : 0);
    result = 31 * result + (eventTimestamp != null ? eventTimestamp.hashCode() : 0);
    result = 31 * result + cost;
    result = 31 * result + limit;
    result = 31 * result + (bucket != null ? bucket.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "AddAndGetRequest{"
        + "resource='"
        + resource
        + '\''
        + ", limitName='"
        + limitName
        + '\''
        + ", property='"
        + property
        + '\''
        + ", expiration="
        + expiration
        + ", eventTimestamp="
        + eventTimestamp
        + ", cost="
        + cost
        + ", bucket="
        + bucket
        + ", limit="
        + limit
        + '}';
  }
}
