package com.example.photoarchive.services;

public interface MongoIdGenerator {
	long generateSequence(String seqName);
}
