package de.adito.aditoweb.nbm.nodejs.impl.options;

import lombok.NonNull;
import org.jetbrains.annotations.*;

import java.util.function.*;

/**
 * Allows easier handling of column name and type in a tableModel. Also allows for easy reordering of columns
 *
 * @author m.kaspera, 01.09.2022
 */
public class TableModelColumn
{

  @NonNull
  private final String name;
  @NonNull
  private final Class<?> classType;
  @Nullable
  private final IsEditableFn isEditableFn;
  @Nullable
  private final GetValueFn getValueFn;
  @Nullable
  private final SetValueFn setValueFn;

  /**
   *
   * @param pName Name of the column
   * @param pClassType type of class that the column consists of
   * @param pIsEditableFn function for determining if a certain cell in the table is editable
   * @param pGetValueFn function for getting the value of cell of this column
   * @param pSetValueFn function for setting the value of a cell of this column
   */
  private TableModelColumn(@NonNull String pName, @NonNull Class<?> pClassType, @Nullable IsEditableFn pIsEditableFn, @Nullable GetValueFn pGetValueFn,
                           @Nullable SetValueFn pSetValueFn)
  {
    name = pName;
    classType = pClassType;
    isEditableFn = pIsEditableFn;
    getValueFn = pGetValueFn;
    setValueFn = pSetValueFn;
  }

  @NonNull
  public String getName()
  {
    return name;
  }

  @NonNull
  public Class<?> getClassType()
  {
    return classType;
  }

  public boolean isEditable(int pIndex)
  {
    if (isEditableFn == null)
      return false;
    return isEditableFn.apply(pIndex);
  }

  @Nullable
  public Object getValue(int pIndex)
  {
    if (getValueFn == null)
      return null;
    return getValueFn.apply(pIndex);
  }

  public void setValue(@NonNull Object pObj, int pIndex)
  {
    if (setValueFn != null)
      setValueFn.accept(pObj, pIndex);
  }

  public static class TableColumnBuilder
  {

    private String name;
    private Class<?> classType;
    private IsEditableFn isEditableFn;
    private GetValueFn getValueFn;
    private SetValueFn setValueFn;

    public TableColumnBuilder setName(@NonNull String pName)
    {
      name = pName;
      return this;
    }

    public TableColumnBuilder setClassType(@NonNull Class<?> pType)
    {
      classType = pType;
      return this;
    }

    public TableColumnBuilder setIsEditableFn(@NonNull IsEditableFn pEditableFn)
    {
      isEditableFn = pEditableFn;
      return this;
    }

    public TableColumnBuilder setGetValueFn(@NonNull GetValueFn pGetValueFn)
    {
      getValueFn = pGetValueFn;
      return this;
    }

    public TableColumnBuilder setSetValueFn(@NonNull SetValueFn pSetValueFn)
    {
      setValueFn = pSetValueFn;
      return this;
    }

    public TableModelColumn build()
    {
      if (name == null)
        throw new IllegalStateException("Argument 'name' can not be null");
      if (classType == null)
        throw new IllegalStateException("Argument 'classType' can not be null");
      return new TableModelColumn(name, classType, isEditableFn, getValueFn, setValueFn);
    }

  }

  public interface IsEditableFn extends Function<Integer, Boolean>
  {
  }

  public interface GetValueFn extends Function<Integer, Object>
  {
  }

  public interface SetValueFn extends BiConsumer<Object, Integer>
  {
  }
}
