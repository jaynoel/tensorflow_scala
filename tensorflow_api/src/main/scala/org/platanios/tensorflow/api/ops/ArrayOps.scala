package org.platanios.tensorflow.api.ops

import org.platanios.tensorflow.api.Exception.{InvalidDataTypeException, InvalidShapeException}
import org.platanios.tensorflow.api.{DataType, Shape, Tensor, using}

/**
  * @author Emmanouil Antonios Platanios
  */
object ArrayOps {
  /** Creates an op that returns a constant tensor.
    *
    * The resulting tensor is populated with values of type `dataType`, as specified by the arguments `value` and
    * (optionally) `shape` (see examples below).
    *
    * The argument `value` can be a constant value, or an array (potentially multi-dimensional) with elements of type
    * `dataType`. If `value` is a one-dimensional array, then its length should be less than or equal to the number of
    * elements implied by the `shape` argument (if specified). In the case where the array length is less than the
    * number of elements specified by `shape`, the last element in the array will be used to fill the remaining entries.
    *
    * The argument `dataType` is optional. If not specified, then its value is inferred from the type of `value`.
    *
    * The argument `shape` is optional. If present, it specifies the dimensions of the resulting tensor. If not present,
    * the shape of `value` is used.
    *
    * @param  tensor      A constant value of data type `dataType`.
    * @param  dataType    Data type of the resulting tensor. If not provided, its value will be inferred from the type
    *                     of `value`.
    * @param  shape       Shape of the resulting tensor.
    * @param  verifyShape If `true` and `shape` is not `null`, then the shape of `value` will be verified (i.e., checked
    *                     to see if it is equal to the provided shape.
    * @param  name        Name for the created op.
    * @return Created op output.
    * @throws InvalidShapeException If `shape != null`, `verifyShape == true`, and the shape of values does not match
    *                               the provided `shape`.
    */
  @throws[InvalidShapeException]
  def constant(
      tensor: Tensor, dataType: DataType = null, shape: Shape = null, verifyShape: Boolean = false,
      name: String = "Constant"): Op.Output = {
    // TODO: Use all provided arguments.
    using(tensor.nativeView) { nativeTensor =>
      Op.Builder(opType = "Const", name = name)
          .setAttribute(name = "value", value = nativeTensor)
          .setAttribute(name = "dtype", value = tensor.dataType)
          .build().outputs(0)
    }
  }

  /** Creates an op that returns a tensor of zeros with the same shape and data type as `input`.
    *
    * @param  input Input.
    * @param  name  Name for the created op.
    * @return Created op output.
    */
  def zerosLike(input: Op.Output, name: String = "ZerosLike"): Op.Output = {
    // TODO: Add support for changing the dataType and for the "optimize" flag.
    Op.Builder(opType = "ZerosLike", name = name)
        .addInput(input)
        .build().outputs(0)
  }

  /** Creates an op that returns a tensor of ones with the same shape and data type as `input`.
    *
    * @param  input Input.
    * @param  name  Name for the created op.
    * @return Created op output.
    */
  def onesLike(input: Op.Output, name: String = "OnesLike"): Op.Output = {
    // TODO: Add support for changing the dataType and for the "optimize" flag.
    Op.Builder(opType = "OnesLike", name = name)
        .addInput(input)
        .build().outputs(0)
  }

  // TODO: Add support for "ImmutableConst".

  // TODO: Add support for "zeros" and "ones", after fixing "Tensor.create".

  /** Creates a placeholder op for a tensor that will always be fed.
    *
    * IMPORTANT NOTE: This op will produce an error if evaluated. Its value must be fed when using `Session.run`. It is
    * intended as a way to represent a value that will always be fed, and to provide attributes that enable the fed
    * value to be checked at runtime.
    *
    * @param  dataType Data type of the elements in the tensor that will be fed.
    * @param  shape    Shape of the tensor that will be fed. The shape can be any partially-specified, or even
    *                  completely unknown.
    * @param  name     Name for the created op.
    * @return Created op output.
    */
  def placeholder(dataType: DataType, shape: Shape = null, name: String = "Placeholder"): Op.Output = {
    val opBuilder = Op.Builder(opType = "Placeholder", name = name)
        .setAttribute(name = "dtype", value = dataType)
    if (shape != null)
      opBuilder.setAttribute(name = "shape", value = shape)
    opBuilder.build().outputs(0)
  }

  /** Creates a placeholder op that passes through `defaultValue` when its input is not fed.
    *
    * @param  defaultValue Default value to pass through when no input is fed for this placeholder.
    * @param  shape        Shape of the tensor that will be fed. The shape can be any partially-specified, or even
    *                      completely unknown.
    * @param  name         Name for the created op.
    * @return Created op output.
    */
  def placeholderWithDefault(defaultValue: Tensor, shape: Shape, name: String = "PlaceholderWithDefault"): Op.Output = {
    Op.Builder(opType = "PlaceholderWithDefault", name = name)
        .addInput(Op.createWith(nameScope = name)(constant(tensor = defaultValue, name = "DefaultValue")))
        .setAttribute(name = "shape", value = shape)
        .build().outputs(0)
  }

  // TODO: [[SPARSE]] Add sparse placeholder.

  /** Creates an op that returns the rank of a tensor.
    *
    * The op returns an integer representing the rank of `input`.
    *
    * For example:
    * {{{
    *   // 't' is [[[1, 1, 1], [2, 2, 2]], [[3, 3, 3], [4, 4, 4]]]
    *   // 't' has shape [2, 2, 3]
    *   rank(t) == 3
    * }}}
    *
    * Note that the rank of a tensor is not the same as the rank of a matrix. The rank of a tensor is the number of
    * indices required to uniquely select each element of the tensor. Rank is also known as order, degree, or number of
    * dimensions.
    *
    * @param  input    Tensor whose rank to return.
    * @param  optimize Boolean flag indicating whether to optimize this op creation by using a constant op with the
    *                  rank value that `input` has at graph creation time (instead of execution time), if known.
    * @param  name     Name for the created op.
    * @return Created op output.
    */
  def rank(input: Op.Output, optimize: Boolean = true, name: String = "Rank"): Op.Output = {
    // TODO: [SPARSE]
    val inputRank = input.shape.rank
    if (optimize && inputRank != -1)
      constant(Tensor.fill(DataType.Int32, Shape())(inputRank), name = name)
    else
      Op.Builder(opType = "Rank", name = name)
          .addInput(input)
          .build().outputs(0)
  }

  /** Creates an op that returns the size of a tensor.
    *
    * The op returns a number representing the number of elements in `input`.
    *
    * For example:
    * {{{
    *   // 't' is [[[1, 1,, 1], [2, 2, 2]], [[3, 3, 3], [4, 4, 4]]]]
    *   size(t) == 12
    * }}}
    *
    * @param  input    Tensor whose size to return.
    * @param  dataType Optional data type to use for the output of this op.
    * @param  optimize Boolean flag indicating whether to optimize this op creation by using a constant op with the
    *                  number of elements provided by the shape of that `input` at graph creation time (instead of
    *                  execution time), if known.
    * @param  name     Name for the created op.
    * @return Created op output.
    */
  def size(
      input: Op.Output, dataType: DataType = DataType.Int32, optimize: Boolean = true,
      name: String = "Size"): Op.Output = {
    // TODO: [SPARSE]
    val inputShape = input.shape
    if (optimize && inputShape.isFullyDefined)
      constant(Tensor.fill(dataType, Shape())(inputShape.numElements.get), name = name)
    else
      Op.Builder(opType = "Size", name = name)
          .addInput(input)
          .setAttribute("out_type", dataType)
          .build().outputs(0)
  }

  /** Creates an op that returns the shape of a tensor.
    *
    * This op returns a one-dimensional tensor representing the shape of `input`.
    *
    * For example:
    * {{{
    *   // 't' is [[[1, 1, 1], [2, 2, 2]], [[3, 3, 3], [4, 4, 4]]]
    *   shape(t) == [2, 2, 3]
    * }}}
    *
    * @param  input    Tensor whose shape to return.
    * @param  dataType Optional data type to use for the output of this op.
    * @param  optimize Boolean flag indicating whether to optimize this op creation by using a constant op with the
    *                  shape of that `input` at graph creation time (instead of execution time), if known.
    * @param  name     Name for the created op.
    * @return Created op output.
    */
  def shape(
      input: Op.Output, dataType: DataType = DataType.Int32, optimize: Boolean = true,
      name: String = "Shape"): Op.Output = {
    // TODO: [SPARSE]
    val inputShape = input.shape
    if (optimize && inputShape.isFullyDefined)
      constant(Tensor(dataType, inputShape.asArray.map(Tensor(_)): _*), name = name) // TODO: [OPTIMIZE]
    else
      Op.Builder(opType = "Shape", name = name)
          .addInput(input)
          .setAttribute("out_type", dataType)
          .build().outputs(0)
  }

  /** Creates an op that returns a tensor with the same shape and contents as the input tensor or value.
    *
    * @param  input Input tensor.
    * @param  name  Name for the created op.
    * @return Created op output.
    */
  def identity(input: Op.Output, name: String = "Identity"): Op.Output = {
    Op.Builder(opType = "Identity", name = name)
        .addInput(input)
        .build().outputs(0)
  }

  /** Creates an op that inserts a dimension of size 1 into a tensor's shape.
    *
    * Given an op output `input`, this op inserts a dimension of size 1 at the dimension index `axis` of `input`'s
    * shape. The dimension index `axis` starts at zero; if you specify a negative number for `axis` it is counted
    * backwards from the end.
    *
    * This op is useful if you want to add a batch dimension to a single element. For example, if you have a single
    * image of shape `[height, width, channels]`, you can make it a batch of 1 image with `expandDims(image, 0)`, which
    * will make the shape equal to `[1, height, width, channels]`.
    *
    * For example:
    * {{{
    *   // 't1' is an op output with shape [2]
    *   shape(expandDims(t1, 0)) == [1, 2]
    *   shape(expandDims(t1, 1)) == [2, 1]
    *   shape(expandDims(t1, -1)) == [2, 1]
    *
    *   // 't2' is a tensor of shape [2, 3, 5]
    *   shape(expandDims(t2, 0)) == [1, 2, 3, 5]
    *   shape(expandDims(t2, 2)) == [2, 3, 1, 5]
    *   shape(expandDims(t2, 3)) == [2, 3, 5, 1]
    * }}}
    *
    * This op requires that `-1 - input.shape.rank <= axis <= input.shape.rank`.
    *
    * This op is related to [[squeeze]], which removes dimensions of size 1.
    *
    * @param  input Input tensor.
    * @param  axis  Dimension index at which to expand the shape of `input`.
    * @param  name  Name for the created op.
    * @return Created op output.
    */
  def expandDims(input: Op.Output, axis: Int, name: String = "ExpandDims"): Op.Output = {
    Op.Builder(opType = "ExpandDims", name = name)
        .addInput(input)
        .addInput(Op.createWith(nameScope = name)(constant(tensor = axis, name = "Axis")))
        .build().outputs(0)
  }

  /** Creates an op that removes dimensions of size 1 from the shape of a tensor.
    *
    * Given a tensor `input`, this op returns a tensor of the same data type, with all dimensions of size 1 removed. If
    * `axes` is specified, then only the dimensions specified by that array will be removed. In that case, all these
    * dimensions need to have size 1.
    *
    * For example:
    * {{{
    *   // 't' is a tensor of shape [1, 2, 1, 3, 1, 1]
    *   shape(squeeze(t)) == [2, 3]
    *   shape(squeeze(t, Array(2L, 4L))) ==> [1, 2, 3, 1]
    * }}}
    *
    * @param  input Input tensor.
    * @param  axes  Dimensions of size 1 to squeeze. If this argument is not provided, then all dimensions of size 1
    *               will be squeezed.
    * @param  name  Name for the created op.
    * @return Created op output.
    */
  def squeeze(input: Op.Output, axes: Array[Int] = null, name: String = "Squeeze"): Op.Output = {
    val builder = Op.Builder(opType = "Squeeze", name = name)
        .addInput(input)
    if (axes != null)
      builder.setAttribute("squeeze_dims", axes.map(_.asInstanceOf[Long]))
    builder.build().outputs(0)
  }

  /** Creates an op that stacks a list of rank-`R` tensors into one rank-`(R+1)` tensor.
    *
    * The op packs the list of tensors in `inputs` into a tensor with rank one higher than each tensor in `inputs`, by
    * packing them along the `axis` dimension. Given a list of `N` tensors of shape `[A, B, C]`:
    *   - If `axis == 0`, then the output tensor will have shape `[N, A, B, C]`.
    *   - If `axis == 1`, then the output tensor will have shape `[A, N, B, C]`.
    *   - If `axis == -1`, then the output tensor will have shape `[A, B, C, N]`.
    *   - etc.
    *
    * For example:
    * {{{
    *   // 'x' is [1, 4]
    *   // 'y' is [2, 5]
    *   // 'z' is [3, 6]
    *   stack(Array(x, y, z)) == [[1, 4], [2, 5], [3, 6]]         // Packed along the first dimension.
    *   stack(Array(x, y, z), axis = 1) == [[1, 2, 3], [4, 5, 6]] // Packed along the second dimension.
    * }}}
    *
    * This op is the opposite of `unstack`.
    *
    * @param  inputs Input tensors to be stacked.
    * @param  axis   Dimension along which to stack the input tensors.
    * @param  name   Name for the created op.
    * @return Created op output.
    * @throws InvalidShapeException     If the input tensor shapes are not compatible with each other.
    * @throws IndexOutOfBoundsException If `axis` is not within the expected output tensor shape rank.
    */
  @throws[InvalidShapeException]
  @throws[IndexOutOfBoundsException]
  def stack(inputs: Array[Op.Output], axis: Int = 0, name: String = "Stack"): Op.Output = {
    val inputsShape = inputs.head.shape
    inputs.tail.foreach(_.shape.assertIsCompatibleWith(inputsShape))
    if (inputsShape.rank != -1) {
      val expandedRank = inputsShape.rank + 1
      if (axis < -expandedRank || axis >= expandedRank)
        throw new IndexOutOfBoundsException(s"Provided axis, $axis, is not in [${-expandedRank}, $expandedRank).")
    }
    Op.Builder(opType = "Pack", name = name)
        .addInputList(inputs)
        .setAttribute("axis", axis)
        .build().outputs(0)
  }

  /** Creates an op that stacks a list of rank-`R` tensors into one rank-`(R+1)` tensor, in parallel.
    *
    * The op packs the list of tensors in `inputs` into a tensor with rank one higher than each tensor in `inputs`, by
    * packing them along the first dimension. Given a list of `N` tensors of shape `[A, B, C]`, the output tensor will
    * have shape `[N, A, B, C]`.
    *
    * For example:
    * {{{
    *   // 'x' is [1, 4]
    *   // 'y' is [2, 5]
    *   // 'z' is [3, 6]
    *   parallelStack(Array(x, y, z)) == [[1, 4], [2, 5], [3, 6]]
    * }}}
    *
    * The op requires that the shape of all input tensors is known at graph construction time.
    *
    * The difference between `stack` and `parallelStack` is that `stack` requires all of the inputs be computed before
    * the operation will begin executing, but does not require that the input shapes be known during graph construction.
    * `parallelStack` will copy pieces of the input into the output as they become available. In some situations this
    * can provide a performance benefit.
    *
    * @param  inputs Input tensors to be stacked.
    * @param  name   Name for the created op.
    * @return Created op output.
    * @throws InvalidShapeException If the input tensor shapes are not compatible with each other.
    */
  @throws[InvalidShapeException]
  def parallelStack(inputs: Array[Op.Output], name: String = "ParallelStack"): Op.Output = {
    val inputsShape = inputs.head.shape
    inputs.tail.foreach(_.shape.assertIsCompatibleWith(inputsShape))
    val outputShape = Shape(inputs.length).concatenateWith(inputsShape)
    Op.Builder(opType = "ParallelConcat", name = name)
        .addInputList(inputs)
        .setAttribute("shape", outputShape)
        .build().outputs(0)
  }

  /** Creates an op that unpacks the provided dimension of a rank-`R` tensor into a list of rank-`(R-1)` tensors.
    *
    * The op unpacks `number` tensors from `input` by chipping it along the `axis` dimension. If `number == -1` (i.e.,
    * unspecified), its value is inferred from the shape of `input`. If `input.shape(axis)` is not known, then an
    * [[IllegalArgumentException]] is thrown.
    *
    * For example, given a tensor of shape `[A, B, C, D]`:
    *   - If `axis == 0`, then the `i`th tensor in the output is the slice `input(i, ::, ::, ::)` and each tensor in the
    * output will have shape `[B, C, D]`.
    *   - If `axis == 1`, then the `i`th tensor in the output is the slice `input(::, i, ::, ::)` and each tensor in the
    * output will have shape `[A, C, D]`.
    *   - If `axis == -1`, then the `i`th tensor in the output is the slice `input(::, ::, ::, i)` and each tensor in
    * the output will have shape `[A, B, C]`.
    *   - etc.
    *
    * This op is the opposite of `stack`.
    *
    * @param  input  Rank `R > 0` `Tensor` to be unstacked.
    * @param  number Number of tensors to unstack. If set to `-1` (the default value), its value will be inferred.
    * @param  axis   Dimension along which to unstack the input tensor.
    * @param  name   Name for the created op.
    * @return Created op outputs.
    * @throws IndexOutOfBoundsException If `axis` is not within the range [-R, R).
    * @throws IllegalArgumentException  If `number` is not specified and its value cannot be inferred.
    */
  @throws[IndexOutOfBoundsException]
  @throws[IllegalArgumentException]
  def unstack(input: Op.Output, number: Int = -1, axis: Int = 0, name: String = "Unstack"): Array[Op.Output] = {
    val num: Int = {
      if (number >= 0) {
        number
      } else {
        val inputShape = input.shape
        val inputShapeRank = inputShape.rank
        if (inputShapeRank != -1 && (axis < -inputShapeRank || axis >= inputShapeRank))
          throw new IndexOutOfBoundsException(
            s"Provided axis, $axis, is not in [${-inputShapeRank}, $inputShapeRank).")
        inputShape(axis).asInstanceOf[Int] // TODO: Make shapes integer-valued instead?
      }
    }
    if (num == -1)
      throw new IllegalArgumentException(s"Cannot infer number of tensors to unstack from shape '${input.shape}'.")
    Op.Builder(opType = "Unpack", name = name)
        .addInput(input)
        .setAttribute("num", num)
        .setAttribute("axis", axis)
        .build().outputs
  }

  /** Creates an op that concatenates tensors along one dimension.
    *
    * The op concatenates the list of tensors `inputs` along the dimension `axis`. If
    * `inputs(i).shape = [D0, D1, ..., Daxis(i), ..., Dn]`, then the concatenated tensor will have shape
    * `[D0, D1, ..., Raxis, ..., Dn]`, where `Raxis = sum(Daxis(i))`. That is, the data from the input tensors is joined
    * along the `axis` dimension.
    *
    * For example:
    * {{{
    *   // 't1' is equal to [[1, 2, 3], [4, 5, 6]]
    *   // 't2' is equal to [[7, 8, 9], [10, 11, 12]]
    *   concat(Array(t1, t2), 0) == [[1, 2, 3], [4, 5, 6], [7, 8, 9], [10, 11, 12]]
    *   concat(Array(t1, t2), 1) == [[1, 2, 3, 7, 8, 9], [4, 5, 6, 10, 11, 12]]
    *
    *   // 't3' has shape [2, 3]
    *   // 't4' has shape [2, 3]
    *   shape(concat(Array(t3, t4), 0)) == [4, 3]
    *   shape(concat(Array(t3, t4), 1)) == [2, 6]
    * }}}
    *
    * Note that, if you want to concatenate along a new axis, it may be better to use the `stack` op instead:
    * {{{
    *   concat(tensors.map(t => expandDims(t, axis)), axis) == stack(tensors, axis)
    * }}}
    *
    * @param  inputs Input tensors to be concatenated.
    * @param  axis   Dimension along which to concatenate the input tensors.
    * @param  name   Name for the created op.
    * @return Created op output.
    */
  def concatenate(inputs: Array[Op.Output], axis: Int = 0, name: String = "Concatenate"): Op.Output = {
    val axisConstant = Op.createWith(nameScope = name)(constant(tensor = axis, name = "Axis"))
    if (inputs.length == 1) {
      Op.createWith(nameScope = name)(identity(inputs.head))
    } else {
      Op.Builder(opType = "ConcatV2", name = name)
          .addInputs(inputs)
          .addInput(axisConstant)
          .build().outputs(0)
    }
  }

  // TODO: Add support for "ConcatOffset".

  /** Creates an op that splits a tensor into sub-tensors.
    *
    * The op splits `input` along dimension `axis` into `numSplits` smaller tensors. It requires that `numSplits` evenly
    * splits `input.shape(axis)`.
    *
    * For example:
    * {{{
    *   // 't' is a tensor with shape [5, 30]
    *   // Split 't' into 3 tensors along dimension 1:
    *   val splits = split(t, numSplits = 3, axis = 1)
    *   shape(splits(0)) == [5, 10]
    *   shape(splits(1)) == [5, 10]
    *   shape(splits(2)) == [5, 10]
    * }}}
    *
    * @param  input     Input tensor to split.
    * @param  numSplits Number of splits to obtain along the `axis` dimension.
    * @param  axis      Dimension along which to split the input tensor.
    * @param  name      Name for the created op.
    * @return Created op outputs.
    */
  def splitEvenly(input: Op.Output, numSplits: Int, axis: Int = 0, name: String = "Split"): Array[Op.Output] = {
    Op.Builder(opType = "Split", name = name)
        .addInput(Op.createWith(nameScope = name)(constant(tensor = axis, name = "Axis")))
        .addInput(input)
        .setAttribute("num_split", numSplits)
        .build().outputs
  }

  /** Creates an op that splits a tensor into sub-tensors.
    *
    * The op splits `input` along dimension `axis` into `splitSizes.length` smaller tensors. The shape of the `i`-th
    * smaller tensor has the same size as the `input` except along dimension `axis` where the size is equal to
    * `splitSizes(i)`.
    *
    * For example:
    * {{{
    *   // 't' is a tensor with shape [5, 30]
    *   // Split 't' into 3 tensors with sizes [4, 5, 11] along dimension 1:
    *   val splits = split(t, splitSizes = [4, 15, 11], axis = 1)
    *   shape(splits(0)) == [5, 4]
    *   shape(splits(1)) == [5, 15]
    *   shape(splits(2)) == [5, 11]
    * }}}
    *
    * @param  input      Input tensor to split.
    * @param  splitSizes Sizes for the splits to obtain.
    * @param  axis       Dimension along which to split the input tensor.
    * @param  name       Name for the created op.
    * @return Created op outputs.
    */
  def split(input: Op.Output, splitSizes: Tensor, axis: Int = 0, name: String = "Split"): Array[Op.Output] = {
    Op.Builder(opType = "SplitV", name = name)
        .addInput(input)
        .addInput(Op.createWith(nameScope = name)(constant(tensor = splitSizes, name = "Sizes")))
        .addInput(Op.createWith(nameScope = name)(constant(tensor = axis, name = "Axis")))
        .build().outputs
  }

  //region Slice Ops

  /** Creates an op that returns a slice from `input`.
    *
    * The op output is a tensor with dimensions described by `size`, whose values are extracted from `input`, starting
    * at the offsets in `begin`.
    *
    * Requirements:
    *
    *   - `0 <= begin(i) <= begin(i) + size(i) <= Di, for i in [0, n)`, where `Di` corresponds to the size of
    * the `i`th dimension of `input` and `n` corresponds to the rank of `input`.
    *
    * @param  input Tensor to slice.
    * @param  begin Begin index tensor (must have data type of `int32` or `int64`). `begin(i)` specifies the offset into
    *               the `i`th dimension of `input` to slice from.
    * @param  size  Slice size tensor (must have data type of `int32` or `int64`). `size(i)` specifies the number of
    *               elements of the `i`th dimension of `input` to slice. If `size(i) == -1`, then all the remaining
    *               elements in dimension `i` are included in the slice (i.e., this is equivalent to setting
    *               `size(i) = input.shape(i) - begin(i)`).
    * @param  name  Name for the created op.
    * @return Created op output.
    */
  def slice(input: Op.Output, begin: Op.Output, size: Op.Output, name: String = "Slice"): Op.Output = {
    if (begin.dataType != DataType.Int32 && begin.dataType != DataType.Int64)
      throw InvalidDataTypeException(s"'begin' data type, '${begin.dataType}', is not 'int32' or 'int64', as required.")
    if (size.dataType != DataType.Int32 && size.dataType != DataType.Int64)
      throw InvalidDataTypeException(s"'size' data type, '${size.dataType}', is not 'int32' or 'int64', as required.")
    Op.Builder(opType = "Slice", name = name)
        .addInput(input)
        .addInput(begin)
        .addInput(size)
        .build().outputs(0)
  }

  /** Creates an op that returns a strided slice from `input`.
    *
    * Note that most users will want to use the `apply` or the `slice` method of [[Op.Output]] rather than this function
    * directly, as the interface of those methods is much simpler.
    *
    * The goal of the op is to produce a new tensor with a subset of the elements from the `n`-dimensional `input`
    * tensor. The subset is chosen using a sequence of `m` sparse specifications encoded into the arguments of this
    * function. Note that, in some cases, `m` could be equal to `n`, but this need not be the case.
    * Each range specification entry can be one of the following:
    *
    *   - An ellipsis (`---` or `Ellipsis`). Ellipses are used to represent zero or more dimensions of a full-dimension
    * selection and are produced using `ellipsisMask`. For example, `foo(---)` is the identity slice.
    *   - A new axis (`NewAxis`). New axes are used to insert new dimensions of size `1` and are produced using
    * `newAxisMask`. For example, `foo(NewAxis, ---)`, where `foo` has shape `[3, 4]`, produces a new tensor with
    * shape `[1, 3, 4]`.
    *   - A single index (`Index`). This is used to keep only elements that have a given index. For example, if `foo` is
    * a tensor with shape `[5, 6]`, `foo(2, ::)` produces a tensor with shape `[6]`. This is encoded in `begin` and
    * `end` (where `end` has to be equal to `begin + 1`) and in the `shrinkAxisMask` (since an axis is being
    * shrinked).
    *   - A slice (`Slice`). Slices define a range with a `start`, an `end`, and a `step` size. They are used to specify
    * which elements to choose from a given dimension. `step` (sometimes called "stride") can be any integer, but
    * `0`. `begin` is an integer which represents the index of the first value to select, while `end` represents the
    * index of the last value to select (exclusive). The number of values selected in each dimension is
    * `end - begin` if `step > 0` and `begin - end` if `step < 0`. `begin` and `end` can be negative, where `-1`
    * corresponds to the last element, `-2` to the second to last, etc. `beginMask` controls whether to replace the
    * explicitly provided `begin` with an implicit effective value of: `0` if `step > 0`, and `-1` if `step < 0`.
    * `endMask` is analogous, but produces the number required to create the largest open interval. There is
    * currently no way to create begin masks and end masks in the Scala Indexer API. Values of `0` and `-1` should
    * instead be appropriately used for the `begin` value. The `endMask` functionality is not currently supported at
    * all since `foo(0 :: )` should return all elements of `foo`, whereas `foo(0 :: -1)` will return all except the
    * last one.
    *
    * Requirements:
    *
    *   - `0 != strides(i),` for `i` in `[0, m)` (i.e., no stride should be equal to `0`).
    *   - `ellipsisMask` must be a power of two (i.e., only one ellipsis used).
    *
    * Each conceptual range specification is encoded in the op's arguments. The encoding is best understood by
    * considering a non-trivial example. In particular:
    *
    * {{{
    *   // 'foo' is a tensor with shape '[5, 5, 5, 5, 5, 5]'
    *   foo(1, 2 :: 4, NewAxis, ---, 0 :: -1 :: -3, ::) will be encoded as:
    *   begin = [1, 2, x, x, 0, x] // Where "x" denotes that this value is ignored (we usually simply set it to 0)
    *   end = [2, 4, x, x, -3, x]
    *   strides = [1, 1, x, x, -1, 1]
    *   beginMask = 1 << 4 | 1 << 5 = 48
    *   endMask = 1 << 5 = 32
    *   ellipsisMask = 1 << 3 = 8
    *   newAxisMask = 1 << 2 = 4
    *   shrinkAxisMask = 1 << 0 = 1
    *   // The final shape of the slice becomes '[2, 1, 5, 5, 2, 5]'
    * }}}
    *
    * Let us walk step by step through each argument specification in the example slice:
    *
    *   1. The first argument is turned into `begin = 1`, `end = begin + 1 = 2`, `strides = 1`, and the first bit of
    * `shrinkAxisMask` set to `1` (i.e., `shrinkAxisMask |= 1 << 0`). Setting the bit of `shrinkAxisMask` to `1`
    * makes sure this argument is treated differently than `1 :: 2`, which would not shrink the corresponding axis.
    *   2. The second argument contributes `2` to `begin`, `4` to `end`, and `1` to `strides`. All masks have zero bits
    * contributed.
    *   3. The third argument sets the third bit of `newAxisMask` to `1` (i.e., `newAxisMask |= 1 << 2`).
    *   4. The fourth argument sets the fourth bit of `ellipsisMask` to `1` (i.e., `ellipsisMask |= 1 << 3`).
    *   5. The fifth argument contributes `0` to `begin`, `-3` to `end`, and `-1` to `strides`. It shows the use of
    * negative indices. A negative index `i` associated with a dimension that has size `s` is converted to a
    * positive index `s + i`. So `-1` becomes `s - 1` (i.e., the last element index). This conversion is done
    * internally and so `begin`, `end`, and `strides` are allowed to have negative values.
    *   6. The sixth argument indicates that the entire contents of the corresponding dimension are selected. It sets
    * the sixth bit of `beginMask` and `endMask` to `1` (i.e., `beginMask |= 1 << 6` and `endMask |= 1 << 6`).
    *
    * @param  input          Tensor to slice.
    * @param  begin          One-dimensional integer tensor. `begin(i)` specifies the begin offset into the `i`th range
    *                        specification. The exact dimension this corresponds to will be determined by context.
    *                        Out-of-bounds values will be silently clamped. If the `i`th bit of `beginMask` is `1`, then
    *                        `begin(i)` is ignored and the full range of the appropriate dimension is used instead.
    *                        Negative values causes indexing to start from the highest element.
    * @param  end            One-dimensional integer tensor. `end(i)` is like `begin(i)` with the exception that it
    *                        determines the end offset into the `i`th range specification, and that `endMask` is used to
    *                        determine full ranges.
    * @param  strides        One-dimensional integer tensor. `strides(i)` specifies the increment in the `i`th range
    *                        specification after extracting a given element. Negative indices will reverse the original
    *                        order. Out-of-bounds values are clamped to `[0, shape(i)) if slice(i) > 0` or
    *                        `[-1, shape(i) - 1] if slice(i) < 0`.
    * @param  beginMask      Integer value representing a bitmask where bit `i` being `1` means to ignore the begin
    *                        value and instead use the largest interval possible. At runtime `begin(i)` will be replaced
    *                        with `[0, shape(i) - 1) if stride(i) > 0` or `[-1, shape(i) - 1]` if `stride(i) < 0`.
    * @param  endMask        Integer value analogous to `beginMask`, but for specifying the end offset of the slice.
    * @param  ellipsisMask   Integer value representing a bitmask where bit `i` being `1` means that the `i`th position
    *                        is actually an ellipsis. At most one bit can be `1`. If `ellipsisMask == 0`, then an
    *                        implicit ellipsis mask with value `1 << (m + 1)` is provided. This means that
    *                        `foo(3 :: 5) == foo(3 :: 5, ---)`. An ellipsis implicitly creates as many range
    *                        specifications as necessary to fully specify the sliced range for every dimension. For
    *                        example, for a 4-dimensional tensor `foo` the slice `foo(2, ---, 5 :: 8)` implies
    *                        `foo(2, ::, ::, 5 :: 8)`.
    * @param  newAxisMask    Integer value representing a bitmask where bit `i` being `1` means that the `i`th range
    *                        specification creates a new dimension with size `1`. For example,
    *                        `foo(0 :: 4, NewAxis, 0 :: 2)` will produce a tensor with shape `[4, 1, 2]`.
    * @param  shrinkAxisMask Integer value representing a bitmask where bit `i` being `1` means that the `i`th range
    *                        specification should shrink the dimensionality. `begin` and `end` must imply a slice of
    *                        size `1` in the dimension. For example, in `foo(0 :: 4, 3, 0 :: 2)` would result in a
    *                        tensor with shape `[4, 2]`.
    * @param  name           Name for the created op.
    * @return Created op output.
    */
  def stridedSlice(
      input: Op.Output, begin: Op.Output, end: Op.Output, strides: Op.Output = null, beginMask: Int = 0,
      endMask: Int = 0, ellipsisMask: Int = 0, newAxisMask: Int = 0, shrinkAxisMask: Int = 0,
      name: String = "StridedSlice"): Op.Output = {
    Op.Builder(opType = "StridedSlice", name = name)
        .addInput(input)
        .addInput(begin)
        .addInput(end)
        .addInput(if (strides == null) onesLike(begin) else strides)
        .setAttribute("begin_mask", beginMask)
        .setAttribute("end_mask", endMask)
        .setAttribute("ellipsis_mask", ellipsisMask)
        .setAttribute("new_axis_mask", newAxisMask)
        .setAttribute("shrink_axis_mask", shrinkAxisMask)
        .build().outputs(0)
  }

  //endregion Slice Ops

  /** Creates an op that tiles the provided input tensor.
    *
    * The op creates a new tensor by replicating `input` `multiples` times. The output tensor's `i`th dimension has
    * `input.shape(i) * multiples(i)` elements, and the values of `input` are replicated `multiples(i)` times along
    * the `i`th dimension. For example, tiling `[a b c d]` by `[2]` produces `[a b c d a b c d]`.
    *
    * @param  input     Tensor to tile.
    * @param  multiples One-dimensional tensor containing the tiling multiples. Its length must be the same as the rank
    *                   of `input`.
    * @param  name      Name for the created op.
    * @return Created op output.
    */
  def tile(input: Op.Output, multiples: Op.Output, name: String = "Tile"): Op.Output = {
    Op.Builder(opType = "Tile", name = name)
        .addInput(input)
        .addInput(multiples)
        .build().outputs(0)
  }

  /** Creates an op that returns locations of `true` values in a boolean tensor.
    *
    * The op returns the coordinates of true elements in `input`. The coordinates are returned in a 2-D tensor where the
    * first dimension (rows) represents the number of true elements, and the second dimension (columns) represents the
    * coordinates of the true elements. Note that the shape of the output tensor can vary depending on how many true
    * values there are in `input`. Indices are output in row-major order.
    *
    * For example:
    * {{{
    *   // 'input' tensor is [[true, false]
    *   //                    [true, false]]
    *   // 'input' has two 'true' values and so the output has two coordinates
    *   // 'input' has rank 2 and so each coordinate has two indices
    *   where(input) == [[0, 0],
    *                    [1, 0]]
    *
    *   // `input` tensor is [[[true, false]
    *   //                     [true, false]]
    *   //                    [[false, true]
    *   //                     [false, true]]
    *   //                    [[false, false]
    *   //                     [false, true]]]
    *   // 'input' has 5 'true' values and so the output has 5 coordinates
    *   // 'input' has rank 3 and so each coordinate has three indices
    *   where(input) == [[0, 0, 0],
    *                    [0, 1, 0],
    *                    [1, 0, 1],
    *                    [1, 1, 1],
    *                    [2, 1, 1]]
    * }}}
    *
    * @param  input Input boolean tensor.
    * @param  name  Name for the created op.
    * @return Created op output.
    */
  def where(input: Op.Output, name: String = "Where"): Op.Output = {
    if (input.dataType != DataType.Bool)
      throw InvalidDataTypeException(
        s"The 'where' op only supports boolean tensors as inputs. It does not support '${input.dataType}' tensors.")
    Op.Builder(opType = "Where", name = name)
        .addInput(input)
        .build().outputs(0)
  }

  /** This method is an alias for the [[MathOps.select]] method (for consistency with respect to the Python API).
    * Please refer to that method's documentation for details.
    *
    * @param  condition Boolean condition tensor.
    * @param  x         Tensor which may have the same shape as `condition`. If `condition` has rank `1`, then `t` may
    *                   have a higher rank, but its first dimension must match the size of `condition`.
    * @param  y         Tensor with the same data type and shape as `t`.
    * @param  name      Name for the created op.
    * @return Created op output.
    */
  def where(condition: Op.Output, x: Op.Output, y: Op.Output, name: String = "Where"): Op.Output = {
    MathOps.select(condition, x, y, name)
  }

  /** Creates an op that reverses variable length slices.
    *
    * This op first slices `input` along the dimension `batchAxis`, and for each slice `i`, it reverses the first
    * `sequenceLengths(i)` elements along the dimension `sequenceAxis`.
    *
    * The elements of `sequenceLengths` must obey `sequenceLengths(i) <= input.shape(sequenceAxis)`, and it must be a
    * vector of length `input.shape(batchAxis)`.
    *
    * The output slice `i` along dimension `batchAxis` is then given by input slice `i`, with the first
    * `sequenceLengths(i)` slices along dimension `sequenceAxis` reversed.
    *
    * For example:
    * {{{
    *   // Given:
    *   // sequenceAxis = 1
    *   // batchAxis = 0
    *   // input.shape = [4, 8, ...]
    *   // sequenceLengths = [7, 2, 3, 5]
    *   // slices of 'input' are reversed on 'sequenceAxis', but only up to 'sequenceLengths':
    *   output(0, 0::7, ---) == input(0, 6::-1::, ---)
    *   output(1, 0::2, ---) == input(1, 1::-1::, ---)
    *   output(2, 0::3, ---) == input(2, 2::-1::, ---)
    *   output(3, 0::5, ---) == input(3, 4::-1::, ---)
    *   // while entries past 'sequenceLengths' are copied through:
    *   output(0, 7::, ---) == input(0, 7::, ---)
    *   output(1, 7::, ---) == input(1, 7::, ---)
    *   output(2, 7::, ---) == input(2, 7::, ---)
    *   output(3, 7::, ---) == input(3, 7::, ---)
    *
    *   // In contrast, given:
    *   // sequenceAxis = 0
    *   // batchAxis = 2
    *   // input.shape = [8, ?, 4, ...]
    *   // sequenceLengths = [7, 2, 3, 5]
    *   // slices of 'input' are reversed on 'sequenceAxis', but only up to 'sequenceLengths':
    *   output(0::7, ::, 0, ---) == input(6::-1::, ::, 0, ---)
    *   output(0::2, ::, 1, ---) == input(1::-1::, ::, 1, ---)
    *   output(0::3, ::, 2, ---) == input(2::-1::, ::, 2, ---)
    *   output(0::5, ::, 3, ---) == input(4::-1::, ::, 3, ---)
    *   // while entries past 'sequenceLengths' are copied through:
    *   output(7::, ::, 0, ---) == input(7::, ::, 0, ---)
    *   output(2::, ::, 1, ---) == input(2::, ::, 1, ---)
    *   output(3::, ::, 2, ---) == input(3::, ::, 2, ---)
    *   output(5::, ::, 3, ---) == input(5::, ::, 3, ---)
    * }}}
    *
    * @param  input           Input tensor to reverse.
    * @param  sequenceLengths One-dimensional tensor with length `input.shape(batchAxis)` and
    *                         `max(sequenceLengths) <= input.shape(sequenceAxis)`.
    * @param  sequenceAxis    Tensor dimension which is partially reversed.
    * @param  batchAxis       Tensor dimension along which the reversal is performed.
    * @param  name            Created op name.
    * @return Created op output which has the same shape as `input`.
    */
  def reverseSequence(
      input: Op.Output, sequenceLengths: Op.Output, sequenceAxis: Int, batchAxis: Int = 0,
      name: String = "ReverseSequence"): Op.Output = {
    Op.Builder(opType = "ReverseSequence", name = name)
        .addInput(input)
        .addInput(sequenceLengths)
        .setAttribute("seq_dim", sequenceAxis)
        .setAttribute("batch_dim", batchAxis)
        .build().outputs(0)
  }

  /** Creates an op that computes the difference between two lists of numbers or strings.
    *
    * Given a list `x` and a list `y`, this operation returns a list `out` that represents all values that are in `x`
    * but not in `y`. The returned list `output` is sorted in the same order that the numbers appear in `x` (duplicates
    * are preserved). This operation also returns a list `indices` that represents the position of each `out` element in
    * `x`. In other words, `output(i) = x(indices(i))`, for `i` in `[0, 1, ..., output.length - 1]`.
    *
    * For example, given inputs `x = [1, 2, 3, 4, 5, 6]` and `y = [1, 3, 5]`, this op would return
    * `output = [2, 4, 6]` and `indices = [1, 3, 5]`.
    *
    * @param  x             One-dimensional tensor containing the values to keep.
    * @param  y             One-dimensional tensor containing the values to remove.
    * @param  indexDataType Optional data type to use for the output indices of this op. It has to be either 'Int32' or
    *                       'Int64'.
    * @param  name          Name for the created op.
    * @return Tuple containing `output` and `indices`, from the method description.
    */
  def listDiff(
      x: Op.Output, y: Op.Output, indexDataType: DataType = DataType.Int32,
      name: String = "ListDiff"): (Op.Output, Op.Output) = {
    if (indexDataType != DataType.Int32 && indexDataType != DataType.Int64)
      throw InvalidDataTypeException(
        s"The index data type cannot be '$indexDataType'. It has to be either 'Int32' or 'Int64'.")
    val outputs = Op.Builder(opType = "ListDiff", name = name)
        .addInput(x)
        .addInput(y)
        .setAttribute("out_idx", indexDataType)
        .build().outputs
    (outputs(0), outputs(1))
  }
}
