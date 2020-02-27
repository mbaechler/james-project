import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

import org.apache.james.blob.api.{BlobId, TestBlobId}
import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuite

case class Deletion(generation: Generation, reference: Reference, date: Instant, iteration: Iteration)

case class Generation(id: Long)
case class Iteration(id: Long)
case class Reference(externalId: ExternalID, blobId: BlobId, generation: Generation)

case class ExternalID(id: String) // TODO

case class Report(iteration: Iteration,
                  blobsToDelete: Set[(Generation, BlobId)]
                 )

object Generators {

  val smallInteger = Gen.choose(0L,100L)
  var current = 0;
//  val generationGen = Gen.listOf(Gen.choose(0,2)).map(list => list.scanLeft(0)(_ + _))
  val generationGen = smallInteger.map(Generation.apply)
  val iterationGen = smallInteger.map(Iteration.apply)

  val blobIdFactory = new TestBlobId.Factory

  def blobIdGen(generation: Generation) : Gen[BlobId] = Gen.uuid.map(uuid =>
    blobIdFactory.from(s"${generation}_$uuid"))

  val externalIDGen = Gen.uuid.map(uuid => ExternalID(uuid.toString))

  val referenceGen = for {
    generation <- generationGen
    blobId <- blobIdGen(generation)
    externalId <- externalIDGen
  } yield Reference(externalId, blobId, generation)

def deletionGen(reference : Ref)

}

class GCPropertiesTest extends AnyFunSuite {
  Generators.generationGen.sample;
  test("youpi") {
    Generators.generationGen.sample;
    println(Generators.referenceGen.sample)
//    Generators.generationGen.retryUntil(x => {
//      println(x);
//      true
//    }, 100)
  }
}
