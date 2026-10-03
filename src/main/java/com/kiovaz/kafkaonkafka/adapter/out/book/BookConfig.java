package com.kiovaz.kafkaonkafka.adapter.out.book;

record BookConfig(String title, String file, String startMarker, String endMarker, String sectionPattern,
                  String chunking, int minLength) {
}
