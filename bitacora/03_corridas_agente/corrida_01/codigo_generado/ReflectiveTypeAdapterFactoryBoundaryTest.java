package com.google.gson.functional;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.ReflectionAccessFilter;
import com.google.gson.ReflectionAccessFilter.FilterResult;
import com.google.gson.annotations.SerializedName;
import org.junit.Test;

public class ReflectiveTypeAdapterFactoryBoundaryTest {

  private static class Parent {
    @SerializedName("shared")
    @SuppressWarnings("unused")
    String parentField = "parentValue";
  }

  private static class Child extends Parent {
    @SerializedName("shared")
    @SuppressWarnings("unused")
    String childField = "childValue";
  }

  @Test
  public void testSerializedNameCollisionInInheritance() {
    Gson gson = new Gson();
    Child child = new Child();
    IllegalArgumentException thrown = assertThrows(
        IllegalArgumentException.class,
        () -> gson.toJson(child)
    );
    assertThat(thrown).hasMessageThat().contains("multiple JSON fields named 'shared'");
  }

  @Test
  public void testSerializedNameCollisionInInheritanceDeserialization() {
    Gson gson = new Gson();
    IllegalArgumentException thrown = assertThrows(
        IllegalArgumentException.class,
        () -> gson.fromJson("{\"shared\":\"value\"}", Child.class)
    );
    assertThat(thrown).hasMessageThat().contains("multiple JSON fields named 'shared'");
  }

  private static class BlockedClass {
    @SuppressWarnings("unused")
    String value = "blocked";
  }

  @Test
  public void testBlockAllFilterThrowsJsonIOException() {
    Gson gson = new GsonBuilder()
        .addReflectionAccessFilter(new ReflectionAccessFilter() {
          @Override
          public FilterResult check(Class<?> rawClass) {
            return rawClass == BlockedClass.class
                ? FilterResult.BLOCK_ALL
                : FilterResult.INDECISIVE;
          }
        })
        .create();

    BlockedClass obj = new BlockedClass();
    JsonIOException thrown = assertThrows(
        JsonIOException.class,
        () -> gson.toJson(obj)
    );
    assertThat(thrown).hasMessageThat()
        .contains("ReflectionAccessFilter does not permit using reflection");
  }

  @Test
  public void testBlockAllFilterDeserialization() {
    Gson gson = new GsonBuilder()
        .addReflectionAccessFilter(new ReflectionAccessFilter() {
          @Override
          public FilterResult check(Class<?> rawClass) {
            return rawClass == BlockedClass.class
                ? FilterResult.BLOCK_ALL
                : FilterResult.INDECISIVE;
          }
        })
        .create();

    JsonIOException thrown = assertThrows(
        JsonIOException.class,
        () -> gson.fromJson("{\"value\":\"test\"}", BlockedClass.class)
    );
    assertThat(thrown).hasMessageThat()
        .contains("ReflectionAccessFilter does not permit using reflection");
  }
}
