/*
 * Copyright 2018-2019 ABSA Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package za.co.absa.enceladus.menas.repositories

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties
import java.util.TimeZone
import scala.concurrent.Future
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path
import org.apache.hadoop.fs.permission.FsAction
import org.apache.hadoop.fs.permission.FsPermission
import org.apache.oozie.client.OozieClient
import org.apache.oozie.client.WorkflowJob.{Status => WorkflowStatus}
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import za.co.absa.enceladus.menas.exceptions.OozieConfigurationException
import za.co.absa.enceladus.menas.models.OozieCoordinatorStatus
import za.co.absa.enceladus.model.Dataset
import za.co.absa.enceladus.model.menas.scheduler.RuntimeConfig
import za.co.absa.enceladus.menas.exceptions.EntityAlreadyExistsException
import za.co.absa.enceladus.utils.time.TimeZoneNormalizer
import OozieRepository._


object OozieRepository {
  private lazy val dateFormat = {
    TimeZoneNormalizer.normalizeJVMTimeZone() //ensure time zone normalization before SimpleDateFormat creation
    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'")
  }
}

@Repository
class OozieRepository @Autowired() (oozieClientRes: Either[OozieConfigurationException, OozieClient],
    datasetMongoRepository: DatasetMongoRepository,
    hadoopFS: FileSystem,
    hadoopConf: Configuration) extends InitializingBean {

  import scala.concurrent.ExecutionContext.Implicits.global

  @Value("${za.co.absa.enceladus.menas.oozie.schedule.hdfs.path:}")
  val oozieScheduleHDFSPath: String = ""

  @Value("${za.co.absa.enceladus.menas.oozie.timeZone:Africa/Ceuta}")
  val oozieTimezone: String = ""

  @Value("${za.co.absa.enceladus.menas.oozie.sharelibForSpark:spark}")
  val oozieShareLib: String = ""

  @Value("${za.co.absa.enceladus.menas.oozie.enceladusJarLocation:}")
  val enceladusJarLocation: String = ""

  @Value("${za.co.absa.enceladus.menas.oozie.mavenStandardizationJarLocation:}")
  val standardizationJarPath: String = ""

  @Value("${za.co.absa.enceladus.menas.oozie.mavenConformanceJarLocation:}")
  val conformanceJarPath: String = ""

  @Value("${za.co.absa.enceladus.menas.oozie.mavenRepoLocation:}")
  val mavenRepoLocation: String = ""

  @Value("${za.co.absa.enceladus.menas.oozie.menasApiURL:}")
  val menasApiURL: String = ""

  @Value("${za.co.absa.enceladus.menas.oozie.splineMongoURL:}")
  val splineMongoURL: String = ""

  @Value("${za.co.absa.enceladus.menas.oozie.sparkConf.surroundingQuoteChar:}")
  val sparkConfQuotes: String = ""

  private val classLoader = Thread.currentThread().getContextClassLoader
  private val workflowTemplate = getTemplateFile("scheduling/oozie/workflow_template.xml")
  private val coordinatorTemplate = getTemplateFile("scheduling/oozie/coordinator_template.xml")
  private val namenode = hadoopConf.get("fs.defaultFS")
  private val resourceManager = hadoopConf.get("yarn.resourcemanager.address")
  private val logger = LoggerFactory.getLogger(this.getClass)

  override def afterPropertiesSet() {
    logger.info(s"Enceladus Jar Location: $enceladusJarLocation")
    logger.info(s"Stanrdardization Jar Path: $standardizationJarPath")
    logger.info(s"Conformance Jar Path: $conformanceJarPath")
    //ensure that relevant jars are properly loaded in HDFS, otherwise initialize
    this.initializeJars()
  }

  private def validateProperties(logWarnings: Boolean = false): Boolean = {
    Seq((oozieScheduleHDFSPath, "za.co.absa.enceladus.menas.oozie.schedule.hdfs.path"),
      (enceladusJarLocation, "zza.co.absa.enceladus.menas.oozie.enceladusJarLocation"),
      (standardizationJarPath, "za.co.absa.enceladus.menas.oozie.mavenStandardizationJarLocation"),
      (conformanceJarPath, "za.co.absa.enceladus.menas.oozie.mavenConformanceJarLocation"),
      (mavenRepoLocation, "za.co.absa.enceladus.menas.oozie.mavenRepoLocation"),
      (menasApiURL, "za.co.absa.enceladus.menas.oozie.menasApiURL"),
      (splineMongoURL, "za.co.absa.enceladus.menas.oozie.splineMongoURL")).map(p => validateProperty(p._1, p._2, logWarnings)).reduce(_ && _)
  }

  private def validateProperty(prop: String, propName: String, logWarnings: Boolean = false): Boolean = {
    if (prop == null || prop.isEmpty) {
      if(logWarnings) {
        logger.warn(s"Oozie support disabled. Missing required configuration property $propName")
      }
      false
    } else {
      true
    }
  }

  private def initializeJars() {
    if (this.isOozieEnabled(true)) {
      val hdfsStdPath = new Path(s"$enceladusJarLocation$standardizationJarPath")
      val hdfsConfPath = new Path(s"$enceladusJarLocation$conformanceJarPath")
      val mavenStdPath = s"$mavenRepoLocation$standardizationJarPath"
      val mavenConfPath = s"$mavenRepoLocation$conformanceJarPath"

      val resFutureStd = this.downloadFile(mavenStdPath, hdfsStdPath)
      resFutureStd.onSuccess {
        case _ => logger.info(s"Standardization jar loaded to $hdfsStdPath")
      }
      resFutureStd.onFailure {
        case err: Throwable =>
          hadoopFS.delete(hdfsStdPath, true)
      }

      val resFutureConf = this.downloadFile(mavenConfPath, hdfsConfPath)
      resFutureConf.onSuccess {
        case _ => logger.info(s"Conformance jar loaded to $hdfsConfPath")
      }
      resFutureConf.onFailure {
        case err: Throwable =>
          hadoopFS.delete(hdfsConfPath, true)
      }

    }
  }

  /**
   * Used for downloading the jar either from maven or local repo
   */
  private def downloadFile(url: String, hadoopPath: Path) = {
    Future {
      if (!hadoopFS.exists(hadoopPath) || hadoopFS.getStatus(hadoopPath).getCapacity == 0) {
        logger.info(s"Uploading jar from $url to $hadoopPath")
        val connection = new URL(url).openConnection()
        connection match {
          case httpConn: HttpURLConnection => httpConn.setRequestMethod("GET")
          case _                           => Unit
        }

        val in = connection.getInputStream
        val targetArray = Array.fill(in.available)(0.toByte)
        in.read(targetArray)
        val os = hadoopFS.create(hadoopPath, true)
        os.write(targetArray)
        os.flush()
        os.close()
      }
    }
  }

  /**
   * Read a template file packaged with the jar
   */
  private def getTemplateFile(fileName: String): String = {
    new BufferedReader(
      new InputStreamReader(
        classLoader.getResourceAsStream(fileName), "UTF-8")).lines().toArray().mkString("\n")
  }

  /**
   * Whether or not oozie is enabled/configured
   */
  def isOozieEnabled(logWarnings: Boolean = false): Boolean = {
    this.validateProperties(logWarnings) && oozieClientRes.isRight
  }

  private def getOozieClient[T](fn: OozieClient => Future[T]): Future[T] = {
    oozieClientRes match {
      case Right(client) => fn(client)
      case Left(ex)      => Future.failed(ex)
    }
  }

  private def getOozieClientWrap[T](fn: OozieClient => T): Future[T] = {
    getOozieClient({ cl: OozieClient =>
      Future(fn(cl))
    })
  }

  /**
   * Get status of submitted coordinater
   */
  def getCoordinatorStatus(coordId: String): Future[OozieCoordinatorStatus] = {
    getOozieClientWrap({ oozieClient: OozieClient =>
      val jobInfo = oozieClient.getCoordJobInfo(coordId)
      val nextMaterializeTime = if (jobInfo.getNextMaterializedTime == null) {
        ""
      } else {
        dateFormat.format(jobInfo.getNextMaterializedTime)
      }
      OozieCoordinatorStatus(jobInfo.getStatus, nextMaterializeTime)
    })
  }

  /**
   * Get status of a running workflow
   */
  def getJobStatus(jobId: String): Future[WorkflowStatus] = {
    getOozieClientWrap({ oozieClient: OozieClient =>
      oozieClient.getJobInfo(jobId).getStatus
    })
  }

  /**
   * Kill a running coordinator
   */
  def killCoordinator(coordId: String): Future[Unit] = {
    getOozieClientWrap({ oozieClient: OozieClient =>
      oozieClient.kill(coordId)
    })
  }

  /**
   * Get workflow from teplate - this fills in all variables and returns representation of the workflow
   */
  private def getWorkflowFromTemplate(ds: Dataset): Array[Byte] = {
    val schedule = ds.schedule.get
    val runtimeParams = schedule.runtimeParams
    workflowTemplate.replaceAllLiterally("$stdAppName", s"Menas Schedule Standardization ${ds.name} (${ds.version})")
      .replaceAllLiterally("$confAppName", s"Menas Schedule Conformance ${ds.name} (${ds.version})")
      .replaceAllLiterally("$stdJarPath", s"$enceladusJarLocation$standardizationJarPath")
      .replaceAllLiterally("$confJarPath", s"$enceladusJarLocation$conformanceJarPath")
      .replaceAllLiterally("$datasetVersion", schedule.datasetVersion.toString)
      .replaceAllLiterally("$datasetName", ds.name)
      .replaceAllLiterally("$mappingTablePattern", schedule.mappingTablePattern.map(_.trim).filter(_.nonEmpty).getOrElse("reportDate={0}-{1}-{2}").trim)
      .replaceAllLiterally("$dataFormat", schedule.rawFormat.name)
      .replaceAllLiterally("$otherDFArguments", schedule.rawFormat.getArguments.map(arg => s"<arg>$arg</arg>").mkString("\n"))
      .replaceAllLiterally("$jobTracker", resourceManager)
      .replaceAllLiterally("$sharelibForSpark", oozieShareLib)
      .replaceAllLiterally("$nameNode", namenode)
      .replaceAllLiterally("$menasRestURI", menasApiURL)
      .replaceAllLiterally("$splineMongoURL", splineMongoURL)
      .replaceAllLiterally("$stdNumExecutors", runtimeParams.stdNumExecutors.toString)
      .replaceAllLiterally("$stdExecutorMemory", s"${runtimeParams.stdExecutorMemory}g")
      .replaceAllLiterally("$confNumExecutors", runtimeParams.confNumExecutors.toString)
      .replaceAllLiterally("$confExecutorMemory", s"${runtimeParams.confExecutorMemory}g")
      .replaceAllLiterally("$driverCores", s"${runtimeParams.driverCores}")
      .replaceAllLiterally("$menasKeytabFile", s"${getCredsOrKeytabArgument(runtimeParams.menasKeytabFile, namenode)}")
      .replaceAllLiterally("$sparkConfQuotes", sparkConfQuotes)
      .getBytes("UTF-8")
  }

  private def getCredsOrKeytabArgument(filename: String, protocol: String): String = {
    if(filename.toLowerCase.trim.endsWith(".properties")) {
      s"""<arg>--menas-credentials-file</arg>
         |<arg>$protocol$filename</arg>""".stripMargin
    } else {
      s"""<arg>--menas-auth-keytab</arg>
         |<arg>$protocol$filename</arg>""".stripMargin
    }
  }

  /**
   * Gets the coordinator from the template, filling in variables
   */
  private def getCoordinatorFromTemplate(ds: Dataset, wfPath: String): Array[Byte] = {
    val schedule = ds.schedule.get
    val runtimeParams = schedule.runtimeParams
    val currentTime = System.currentTimeMillis()
    val futureTime = currentTime + 3.1573e12.toLong
    val timezoneOffset = TimeZone.getTimeZone(oozieTimezone).getOffset(currentTime)
    val startDate = new Date(currentTime)
    val endDate = new Date(futureTime)
    coordinatorTemplate.replaceAllLiterally("$coordName", s"Menas Schedule Coordinator ${ds.name} (${ds.version})")
      .replaceAllLiterally("$cronTiming", schedule.scheduleTiming.getCronSchedule)
      .replaceAllLiterally("$reportDateOffset", schedule.reportDateOffset.toString)
      .replaceAllLiterally("$timezone", oozieTimezone)
      .replaceAllLiterally("$startDate", dateFormat.format(startDate))
      .replaceAllLiterally("$endDate", dateFormat.format(endDate))
      .replaceAllLiterally("$wfApplicationPath", wfPath).getBytes("UTF-8")
  }

  /**
   * Get oozie properties
   */
  private def getOozieConf(oozieClient: OozieClient, runtimeParams: RuntimeConfig): Properties = {
    val conf = oozieClient.createConfiguration()
    conf.setProperty("jobTracker", resourceManager)
    conf.setProperty("nameNode", namenode)
    conf.setProperty(OozieClient.USE_SYSTEM_LIBPATH, "True")
    conf.setProperty("send_email", "False")
    conf.setProperty("mapreduce.job.user.name", runtimeParams.sysUser)
    conf.setProperty("security_enabled", "False")
    conf.setProperty("user.name", runtimeParams.sysUser)
    conf
  }

  /**
   * Submits a coordinator
   */
  def runCoordinator(coordPath: String, runtimeParams: RuntimeConfig): Future[String] = {
    getOozieClientWrap { oozieClient: OozieClient =>
      val conf = getOozieConf(oozieClient, runtimeParams)
      conf.setProperty(OozieClient.COORDINATOR_APP_PATH, s"$coordPath");
      // submit and start the workflow job
      oozieClient.submit(conf)
    }
  }

  /**
   * Helper function which writes a workflow/coordinator data and opens up permissions
   */
  private def writeScheduleData(path: String, content: Array[Byte]): Future[String] = {
    Future {
      val p = new Path(path)
      if (hadoopFS.exists(p)) {
        throw EntityAlreadyExistsException(s"Schedule already exists! Please delete $path and/or try again")
      }
      val os = hadoopFS.create(p)
      os.write(content)
      os.flush()
      os.close()

      hadoopFS.setPermission(new Path(path), new FsPermission(FsAction.ALL, FsAction.ALL, FsAction.ALL))

      path
    }
  }

  /**
   * Create a new workflow
   *
   * @return Workflow path
   */
  def createWorkflow(dataset: Dataset): Future[String] = {
    val wfPath = s"$namenode$oozieScheduleHDFSPath/menas-oozie-schedule-wf-${dataset.name}-${dataset.version + 1}/workflow.xml"
    val content = getWorkflowFromTemplate(dataset)
    this.writeScheduleData(wfPath, content)
  }

  /**
   * Create a new coordinator
   *
   * @return Coordinator path
   */
  def createCoordinator(dataset: Dataset, wfPath: String): Future[String] = {
    val coordPath = s"$namenode$oozieScheduleHDFSPath/menas-oozie-schedule-coord-${dataset.name}-${dataset.version + 1}/coordinator.xml"
    val content = getCoordinatorFromTemplate(dataset, wfPath)
    this.writeScheduleData(coordPath, content)
  }
}
