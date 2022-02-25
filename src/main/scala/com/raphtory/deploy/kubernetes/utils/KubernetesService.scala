package com.raphtory.deploy.kubernetes.utils

import io.fabric8.kubernetes.api.model.IntOrString
import io.fabric8.kubernetes.api.model.Service
import io.fabric8.kubernetes.api.model.ServiceBuilder
import io.fabric8.kubernetes.client.KubernetesClient

import scala.collection.JavaConverters._

object KubernetesService {

  def get(
      client: KubernetesClient,
      namespace: String,
      name: String
  ): Service =
    client.services.inNamespace(namespace).withName(name).get

  def build(
      name: String,
      selectorLabels: Map[String, String],
      annotations: Map[String, String] = Map(),
      labels: Map[String, String],
      portName: String,
      portProtocol: String,
      port: Int,
      targetPort: Int,
      serviceType: String
  ): Service =
    new ServiceBuilder()
      .withNewMetadata()
      .withName(name)
      .addToAnnotations(annotations.asJava)
      .addToLabels(labels.asJava)
      .endMetadata()
      .withNewSpec()
      .withSelector(selectorLabels.asJava)
      .addNewPort()
      .withName(portName)
      .withProtocol(portProtocol)
      .withPort(port)
      .withTargetPort(new IntOrString(targetPort))
      .endPort()
      .withType(serviceType)
      .endSpec()
      .build();

  //      .addToLabels("expose", "true")

  def create(
      client: KubernetesClient,
      namespace: String,
      serviceConfig: Service
  ): Service =
    client.services().inNamespace(namespace).createOrReplace(serviceConfig)

  def delete(
      client: KubernetesClient,
      namespace: String,
      name: String
  ): Boolean =
    client.services().inNamespace(namespace).withName(name).delete();
}