from setuptools import setup, find_packages

setup(
    name="yall",
    version="0.1.0",
    author="BroncBotz 3481",
    description="An improved version of the LimelightHelpers script released by LimelightVision.",
    packages=find_packages(),
    install_requires=[
        "robotpy-wpimath",
        "pyntcore",
    ],
    python_requires=">=3.8",
)
