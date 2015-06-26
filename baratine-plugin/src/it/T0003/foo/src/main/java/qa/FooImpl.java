package qa;

import io.baratine.core.*;

@Service("public://bar/bar-service")
public class BarImpl
{
  public void test(Result<String> result) {
    result.complete("hello world!");
  }
}