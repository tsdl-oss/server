package org.tsdl.client.writer;

import org.tsdl.client.CsvSerializingQueryClientSpecification;
import org.tsdl.client.QueryClientSpecification;
import org.tsdl.client.QueryResultWriter;
import org.tsdl.infrastructure.common.Condition;
import org.tsdl.infrastructure.common.Conditions;
import org.tsdl.infrastructure.model.QueryResult;
import org.tsdl.infrastructure.model.TsdlPeriod;

/**
 * A CSV {@link QueryResultWriter} for {@link TsdlPeriod} results.
 */
public class PeriodWriter extends BaseWriter {
  @Override
  public void write(QueryResult result, QueryClientSpecification specification) {
    safeWriteOperation(() -> {
      verifyTypes(result, specification);

      Conditions.checkNotNull(Condition.ARGUMENT, result, "Result must not be null.");
      Conditions.checkNotNull(Condition.ARGUMENT, specification, "Specification must not be null.");

      try (var csvWriter = createWriter(((CsvSerializingQueryClientSpecification) specification).targetFile())) {
        writeDiscriminatorComment(csvWriter, result);

        csvWriter.writeRow("index", "empty", "start", "end");
        var res = ((TsdlPeriod) result);
        csvWriter.writeRow(res.index().toString(), Boolean.toString(res.isEmpty()), res.start().toString(), res.end().toString());

        writeLogs(csvWriter, result.logs());
      }
    });
  }

  @Override
  Class<? extends QueryResult> resultClass() {
    return TsdlPeriod.class;
  }

  @Override
  Class<? extends QueryClientSpecification> specificationClass() {
    return CsvSerializingQueryClientSpecification.class;
  }
}
