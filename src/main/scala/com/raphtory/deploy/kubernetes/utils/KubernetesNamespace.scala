package com.raphtory.deploy.kubernetes.utils

import io.fabric8.kubernetes.api.model.Namespace
import io.fabric8.kubernetes.api.model.NamespaceBuilder
import io.fabric8.kubernetes.client.KubernetesClient
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.collection.JavaConverters._

object KubernetesNamespace {

  def get(
      client: KubernetesClient,
      name: String
  ): Namespace =
    client.namespaces.withName(name).get

  def listAll(
      client: KubernetesClient
  ): ListBuffer[String] = {
    val namespaces = mutable.ListBuffer[String]()
    client.namespaces().list.getItems.forEach(x => namespaces += x.getMetadata.getName)
    namespaces
  }

  def create(
      client: KubernetesClient,
      name: String,
      labels: Map[String, String] = Map()
  ): Namespace = {
    val ns = new NamespaceBuilder().withNewMetadata
      .withName(name)
      .addToLabels(labels.asJava)
      .endMetadata
      .build

    client.namespaces().createOrReplace(ns)
  }

  def delete(
      client: KubernetesClient,
      name: String
  ): Boolean =
    client.namespaces.withName(name).delete()
}