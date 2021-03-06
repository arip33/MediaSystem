package hs.mediasystem.db;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Blob;
import java.sql.SQLException;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface Column {
  String[] name() default {};
  Class<?> converterClass() default DefaultConverter.class;

  public static class DefaultConverter implements DataTypeConverter<Object, Object> {
    @Override
    public Object toStorageType(Object input) {
      return input;
    }

    @Override
    public Object toJavaType(Object input, Class<?> type) {
      try {
        if(type.isEnum() && input instanceof String) {
          Method method = type.getMethod("valueOf", String.class);

          return method.invoke(null, input);
        }
        else if(input instanceof Blob) {
          Blob blob = (Blob)input;

          return blob.getBytes(1L, (int)blob.length());
        }

        return input;
      }
      catch(InvocationTargetException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | SQLException e) {
        throw new RuntimeException("Exception during conversion to " + type + ": " + input, e);
      }
    }
  }
}
