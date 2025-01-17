package com.raphtory.api.output.format

import com.raphtory.api.output.sink.SinkConnector
import com.raphtory.api.output.sink.SinkExecutor
import com.typesafe.config.Config

/** Base trait for formats
  *
  * A format is a way to translate a `Table` into one or more text items.
  *
  * An item is a piece of information meaningful by itself and isolated from all other entities from the perspective of the format.
  * For instance, in CSV format, an item might be just a row of the CSV table,
  * whereas if the entire table is written out within just one JSON object, the entire JSON object would be an item.
  *
  * Implementations of this trait need to override the `defaultDelimiter` and `executor` methods.
  * These shouldn't write out any data by themselves, instead they should rely on the provided `SinkConnector` to output the items.
  * This allows the format to be used in combination with any implemented sink.
  *
  * @see [[com.raphtory.api.analysis.table.Table Table]]
  *      [[com.raphtory.api.output.sink.Sink Sink]]
  *      [[com.raphtory.api.output.sink.SinkExecutor SinkExecutor]]
  *      [[com.raphtory.api.output.sink.SinkConnector SinkConnector]]
  *      [[com.raphtory.formats.CsvFormat CsvFormat]]
  *      [[com.raphtory.formats.JsonFormat JsonFormat]]
  */
trait Format {

  /** Returns the item delimiter to be used by the `SinkConnector` to separate the items if necessary */
  def defaultDelimiter: String

  /** Returns the default file extension to use if needed, e.g. 'csv' for a CSV file */
  def defaultExtension: String

  /** Creates a `SinkExecutor` implementing the actual operation of the format
    * @param connector the `SinkConnector` to be used by this format to write out the items
    * @param jobID the ID of the job that generated the table
    * @param partitionID the ID of the partition of the table
    * @param config the configuration provided by the user
    * @return the `SinkExecutor` to be used for writing out results
    */
  def executor(
      connector: SinkConnector,
      jobID: String,
      partitionID: Int,
      config: Config
  ): SinkExecutor
}
